//go:build novpn

// lantern-hop runs Lantern's client core (Radiance, GPL-3.0) as a local SOCKS proxy for Dr VPN's
// extra hop. Built with the "novpn" tag, Radiance opens no TUN device: its only entry point is a
// SOCKS/HTTP inbound at --socks. The process connects with Lantern's automatic server choice and
// stays until it is killed. Telemetry stays off; sharing the connection (Unbounded) is never enabled.
package main

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"flag"
	"log"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"syscall"
	"time"

	"github.com/getlantern/radiance/backend"
	"github.com/getlantern/radiance/vpn"
)

func main() {
	socks := flag.String("socks", "127.0.0.1:10830", "address of the local SOCKS/HTTP proxy")
	dataDir := flag.String("data", "", "folder for Lantern's state (device ID, config)")
	logLevel := flag.String("log-level", "info", "trace, debug, info, warn, error")
	locale := flag.String("locale", "fa-IR", "locale sent to Lantern's config server")
	flag.Parse()
	if *dataDir == "" {
		log.Fatal("--data is required")
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	if err := os.MkdirAll(*dataDir, 0o700); err != nil {
		log.Fatalf("data folder: %v", err)
	}
	deviceID, err := loadDeviceID(*dataDir)
	if err != nil {
		log.Fatalf("device ID: %v", err)
	}

	b, err := backend.NewLocalBackend(ctx, backend.Options{
		DataDir:          *dataDir,
		DeviceID:         deviceID,
		LogDir:           filepath.Join(*dataDir, "logs"),
		Locale:           *locale,
		LogLevel:         *logLevel,
		TelemetryConsent: false,
		EnvOverrides:     map[string]string{"RADIANCE_SOCKS_ADDRESS": *socks},
	})
	if err != nil {
		log.Fatalf("start: %v", err)
	}
	b.Start()

	// The first config comes from Lantern's API (through its own fronted transports), which can
	// take a while on a filtered network: keep trying until connected or stopped.
	for {
		cctx, cancel := context.WithTimeout(ctx, 2*time.Minute)
		err = b.ConnectVPN(cctx, vpn.AutoSelectTag)
		cancel()
		if err == nil {
			log.Printf("connected, SOCKS on %s", *socks)
			break
		}
		log.Printf("connect: %v", err)
		select {
		case <-ctx.Done():
			b.Close()
			return
		case <-time.After(5 * time.Second):
		}
	}

	<-ctx.Done()
	_ = b.DisconnectVPN()
	b.Close()
}

// loadDeviceID returns this install's random device ID, made on first run. On Android Radiance
// does not make one itself (the Lantern app passes its own), and Lantern's API refuses a new
// free user without it.
func loadDeviceID(dataDir string) (string, error) {
	path := filepath.Join(dataDir, "device-id")
	if b, err := os.ReadFile(path); err == nil {
		if id := strings.TrimSpace(string(b)); id != "" {
			return id, nil
		}
	}
	raw := make([]byte, 16)
	if _, err := rand.Read(raw); err != nil {
		return "", err
	}
	id := hex.EncodeToString(raw)
	return id, os.WriteFile(path, []byte(id), 0o600)
}
