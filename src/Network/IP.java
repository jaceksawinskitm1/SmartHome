package Network;

import java.util.Arrays;

public class IP {
    private int address;


    public byte[] getAddress() {
        byte[] arr = new byte[4];
        arr[3] = (byte)((this.address << 24) >> 24);
        arr[2] = (byte)((this.address << 16) >> 24);
        arr[1] = (byte)((this.address << 8) >> 24);
        arr[0] = (byte)(this.address >> 24);
        return arr;
    }

    public int getAddressRaw() {
        return this.address;
    }

    public void setAddress(byte[] address) {
        if (address.length != 4) {
            throw new RuntimeException("Invalid Network.IP address");
        }
        this.address = (address[0] & 0xFF);

        this.address = this.address << 8;

        this.address |= (address[1] & 0xFF);
        this.address = this.address << 8;
        this.address |= (address[2] & 0xFF);
        this.address = this.address << 8;
        this.address |= (address[3] & 0xFF);
    }

    public void setAddressRaw(int address) {
        this.address = address;
    }

    public IP() {}

    public IP(byte[] address) {
        setAddress(address);
    }

    public IP(String addressString) {
        String[] split = addressString.split("\\.");
        if (split.length != 4)
            throw new RuntimeException("Invalid Network.IP address string " + addressString);

        byte[] address = new byte[] {
                (byte) (Integer.parseInt(split[3]) & 0xFF),
                (byte) (Integer.parseInt(split[2]) & 0xFF),
                (byte) (Integer.parseInt(split[1]) & 0xFF),
                (byte) (Integer.parseInt(split[0]) & 0xFF)
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
