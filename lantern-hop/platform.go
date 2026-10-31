//go:build novpn

package main

import (
	"errors"
	"flag"
	"strings"
	"syscall"

	"github.com/sagernet/sing-box/experimental/libbox"
)

// On Android an app may not open netlink sockets, which sing-box uses to watch the network.
// Lantern's own app answers those questions from Java through libbox.PlatformInterface; here the
// app passes the network it uses on the command line instead (LanternManager), and a network
// change restarts this process.
var (
	ifaceName  = flag.String("iface", "", "the default network interface (Android: needed, netlink is banned)")
	ifaceIndex = flag.Int("iface-index", -1, "its index")
	ifaceMTU   = flag.Int("iface-mtu", 1500, "its MTU")
	ifaceAddrs = flag.String("iface-addrs", "", "its addresses, comma separated prefixes (10.0.2.16/24,...)")
	ifaceType  = flag.String("iface-type", "wifi", "wifi, cellular or ethernet")
)

// platform is a libbox.PlatformInterface with only the network questions answered.
type platform struct{}

func (platform) iface() *libbox.NetworkInterface {
	var addrs []string
	for _, a := range strings.Split(*ifaceAddrs, ",") {
		if a = strings.TrimSpace(a); a != "" {
			addrs = append(addrs, a)
		}
	}
	kind := libbox.InterfaceTypeWIFI
	switch *ifaceType {
	case "cellular":
		kind = libbox.InterfaceTypeCellular
	case "ethernet":
		kind = libbox.InterfaceTypeEthernet
	}
	return &libbox.NetworkInterface{
		Index:     int32(*ifaceIndex),
		MTU:       int32(*ifaceMTU),
		Name:      *ifaceName,
		Addresses: &strings_{addrs},
		Type:      kind,
		DNSServer: &strings_{},
		Metered:   *ifaceType == "cellular",
		// Up and running: sing-box leaves out interfaces that aren't up.
		Flags: int32(syscall.IFF_UP | syscall.IFF_RUNNING | syscall.IFF_BROADCAST | syscall.IFF_MULTICAST),
	}
}

func (platform) LocalDNSTransport() libbox.LocalDNSTransport { return nil }
func (platform) UsePlatformAutoDetectInterfaceControl() bool { return false }
func (platform) AutoDetectInterfaceControl(int32) error      { return nil }
func (platform) OpenTun(libbox.TunOptions) (int32, error) {
	return -1, errors.New("no TUN in lantern-hop")
}
func (platform) UseProcFS() bool                                                   { return false }
func (platform) UnderNetworkExtension() bool                                       { return false }
func (platform) IncludeAllNetworks() bool                                          { return false }
func (platform) ReadWIFIState() *libbox.WIFIState                                  { return nil }
func (platform) SystemCertificates() libbox.StringIterator                         { return &strings_{} } // empty: Go's system pool
func (platform) ClearDNSCache()                                                    {}
func (platform) SendNotification(*libbox.Notification) error                       { return nil }
func (platform) RestartService() error                                             { return nil }
func (platform) PostServiceClose()                                                 {}
func (platform) CloseDefaultInterfaceMonitor(libbox.InterfaceUpdateListener) error { return nil }

func (platform) FindConnectionOwner(int32, string, int32, string, int32) (*libbox.ConnectionOwner, error) {
	return nil, errors.New("not supported")
}

func (p platform) StartDefaultInterfaceMonitor(listener libbox.InterfaceUpdateListener) error {
	if *ifaceName == "" || *ifaceIndex < 0 {
		listener.UpdateDefaultInterface("", -1, false, false)
		return nil
	}
	listener.UpdateDefaultInterface(*ifaceName, int32(*ifaceIndex), *ifaceType == "cellular", false)
	return nil
}

func (p platform) GetInterfaces() (libbox.NetworkInterfaceIterator, error) {
	if *ifaceName == "" || *ifaceIndex < 0 {
		return &ifaces{}, nil
	}
	return &ifaces{[]*libbox.NetworkInterface{p.iface()}}, nil
}

type strings_ struct{ v []string }

func (s *strings_) Len() int32    { return int32(len(s.v)) }
func (s *strings_) HasNext() bool { return len(s.v) > 0 }
func (s *strings_) Next() string {
	if len(s.v) == 0 {
		return ""
	}
	n := s.v[0]
	s.v = s.v[1:]
	return n
}

type ifaces struct{ v []*libbox.NetworkInterface }

func (s *ifaces) HasNext() bool { return len(s.v) > 0 }
func (s *ifaces) Next() *libbox.NetworkInterface {
	if len(s.v) == 0 {
		return nil
	}
	n := s.v[0]
	s.v = s.v[1:]
	return n
}
