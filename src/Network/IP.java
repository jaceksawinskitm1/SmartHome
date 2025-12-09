package Network;

public class IP {
    private int address;

    public byte[] getAddress() {
        byte[] arr = new byte[4];
        arr[4]
    }

    public void setAddress(byte[] address) {
        if (address.length != 4) {
            throw new RuntimeException("Invalid Network.IP address");
        }
        this.address = address;
    }

    public IP() {}

    public IP(byte[] address) {
        this.address = address;
    }

    public IP(String addressString) {
        String[] split = addressString.split("\\.");
        if (split.length != 4)
            throw new RuntimeException("Invalid Network.IP address string " + addressString);

        byte[] address = new byte[] {
                Byte.parseByte(split[0]),
                Byte.parseByte(split[1]),
                Byte.parseByte(split[2]),
                Byte.parseByte(split[3])
        };
        this.setAddress(address);
    }


    public IP getNext() {
        byte[] address = this.getAddress().clone();

        // TODO: Add octet switching
        address[address.length - 1] += 1;

        return new IP(address);
    }
}
