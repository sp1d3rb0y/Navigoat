package fr.spiderboy.navigoat;

/**
 * Created by pbni on 4/15/15.
 */
public class APDU {

    private static final byte CLA = (byte) 0x94;
    private byte[] value;

    public static enum status {
        OK,
        SECURITY_NOT_OK,
        RECORD_NOT_FOUND,
        COMMAND_NOT_ALLOWED,
        FILE_NOT_FOUND,
        WRONG_PARAMETER,
        BAD_LENGTH,
        BAD_LENGTH_WITH_CORRECTION,
        HAS_RESPONSE,
        PIN_REQUIRED,
        UNKNOWN;
    };

    public static enum ins {
        SELECT_FILE((byte) 0xa4),
        SELECT_FILE_PARAM((byte) 0x08),
        READ_RECORD((byte) 0xb2),
        READ_RECORD_MODE((byte) 0x04);

        private final byte value;

        private ins(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    };

    public APDU(byte INS, byte[] args) {
        byte[] beginning = {
                CLA,
                INS,
        };
        value = new byte[beginning.length + args.length];
        System.arraycopy(beginning, 0, value, 0, beginning.length);
        System.arraycopy(args, 0, value, beginning.length, args.length);
    }

    public byte[] getValue() {
        return value;
    }

    public String toString() {
        return toString(value);
    }

    public static String toString(byte[] t) {
        if (t != null) {
            StringBuilder sb = new StringBuilder(t.length * 2);
            for (byte b : t) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    public static String toBinaryString(byte[] t) {
        /// Only work til t.length - 2, since we don't want to represent the status response sent
        /// by the card
        if (t != null) {
            StringBuilder sb = new StringBuilder((t.length - 2) * Byte.SIZE);
            for (int i = 0; i < Byte.SIZE * (t.length - 2); i++) {
                sb.append((t[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    public static status getStatus(byte[] response) {
        byte[] lastBytes = {response[response.length -2],
                            response[response.length -1]};
        switch ((byte) lastBytes[0]) {
            case (byte) 0x90:
                if (lastBytes[1] == (byte)0x0) {
                    return status.OK;
                }
                break;
            case (byte) 0x69:
                if (lastBytes[1] == (byte) 0x82) {
                    return status.SECURITY_NOT_OK;
                } else if (lastBytes[1] == (byte) 0x86) {
                    return status.COMMAND_NOT_ALLOWED;
                }
                break;
            case (byte) 0x6a:
                if (lastBytes[1] == (byte) 0x83) {
                    return status.RECORD_NOT_FOUND;
                } else if (lastBytes[1] == (byte) 0x82) {
                    return status.FILE_NOT_FOUND;
                } else if (lastBytes[1] == (byte) 0x86) {
                    return status.WRONG_PARAMETER;
                }
                break;
            case (byte) 0x67:
                return status.BAD_LENGTH;
            case (byte) 0x6c:
                return status.BAD_LENGTH_WITH_CORRECTION;
            case (byte) 0x9f:
                return status.HAS_RESPONSE;
            case (byte) 0x98:
                if (lastBytes[1] == (byte)0x04 || lastBytes[1] == (byte)0x40) {
                    return status.PIN_REQUIRED;
                }
                break;
            default:
                return status.UNKNOWN;
        };
        return status.UNKNOWN;
    }
}
