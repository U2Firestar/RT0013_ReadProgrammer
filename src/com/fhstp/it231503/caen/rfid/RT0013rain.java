package com.fhstp.it231503.caen.rfid;

import com.caen.RFIDLibrary.*;

import static com.fhstp.it231503.caen.util.Helper.shortsToBytes;

/**
 * Low Level Communication Implementation for QLOG CAEN RT0013 RFID TAG.
 *
 * @author Emil Sedlacek / it231503
 * @author CAEN
 * @implNote Using CAEN API 5.0.0
 * @see "CAEN Technical Information"
 */
public class RT0013rain implements RT0013 {
    /**
     * CAEN API wrapper for tag access
     */
    private CAENRFIDTag tag;
    /**
     * CAEN API virtual channel for tag communication
     */
    private CAENRFIDLogicalSource mySource;
    /**
     * Physical channel for CAEN API communication
     */
    private final CAENRFIDReader myReader = new CAENRFIDReader();

    public CAENRFIDTag getTag() {
        return tag;
    }

    public void setTag(CAENRFIDTag tag) {
        this.tag = tag;
    }

    /**
     * Function for opening up the communication channel to reader.
     *
     * @param COMPORT Windows COM-Port of RFID reader
     * @throws RuntimeException When init fails.
     */
    public void openRessources(String COMPORT) {
        try {
            myReader.Connect(CAENRFIDPort.CAENRFID_RS232, COMPORT); // args[0] example: "COM4"
            System.out.println("Reader connected successfully.");
            try {
                mySource = myReader.GetSource("Source_0");
            } catch (CAENRFIDException e) {
                throw new RuntimeException("Failed to get source: " + e.getMessage(), e);
            }
        } catch (CAENRFIDException e) {
            throw new RuntimeException("Failed to connect reader: " + e.getMessage(), e);
        }
    }

    /**
     * Function for closing up the communication channel to reader and its channel.
     *
     * @throws RuntimeException When deinit fails.
     */
    public void closeResources() {
        try {
            mySource = null;
            myReader.Disconnect();
        } catch (CAENRFIDException e) {
            throw new RuntimeException("Failed to disconnect reader: " + e.getMessage(), e);
        }
    }

    /**
     * Function for scanning for RFID tags.
     *
     * @throws RuntimeException Fails in case of critical API error. Does NOT mean finding nothing!
     */
    public CAENRFIDTag[] doInventory() {
        try {
            return mySource.InventoryTag();
        } catch (CAENRFIDException e) {
            throw new RuntimeException("Failed to inventory tags: " + e.getMessage(), e);
        }
    }

    /**
     * Low level communication implementation for QLOG CAEN RT0013 RFID TAG. Provides Low level Communication according to RT0013 Datasheet
     *
     * @author CAEN
     * @implNote Using CAEN API 5.0.0
     * @see "CAEN Technical Information"
     */
    private static class INTERFACEMEM {
        public static final short CMDBANK = 3;
        public static final short TRIGBANK = 1;
        public static final short ADDR_TRIGGER = 0x001F * 2; //byte address
        public static final short ADDR_COMMAND = 0;
        public static final short ADDR_ADDRESS = (short) (ADDR_COMMAND + 2);
        public static final short ADDR_SIZE = (short) (ADDR_ADDRESS + 2);
        public static final short ADDR_REPLY = (short) (ADDR_SIZE + 2);
        public static final short ADDR_DATA = (short) (ADDR_REPLY + 2);
        public static final short MAXBYTESIZEDATA = 200 * 2;
        public static final byte CMD_READ = 0x12;
        public static final byte CMD_WRITE = 0x13;
        public static final byte REPLY_ACK = (byte) 0xAC;
        public static final byte REPLY_NACK = (byte) 0xFC;

        /**
         * This function loads the command, address, and size parameters in the corresponding registers of tag memory interface.
         */
        public static void interfacemem_writeparams(CAENRFIDLogicalSource LS_0, CAENRFIDTag tag, short command, short wordaddress, short words) throws Exception {
            byte[] data = new byte[6];
            data[0] = (byte) (command >> 8);
            data[1] = (byte) (command & 0xFF);
            data[2] = (byte) (wordaddress >> 8);
            data[3] = (byte) (wordaddress & 0xFF);
            data[4] = (byte) (words >> 8);
            data[5] = (byte) (words & 0xFF);
            try {
                LS_0.WriteTagData_EPC_C1G2(tag, CMDBANK, ADDR_COMMAND, (short) data.length, data);
            } catch (Exception ex) {
                throw new Exception(ex);
            }
        }

        /**
         * This function triggers the tag parsing and execution of a command
         */
        public static void interfacemem_trigger(CAENRFIDLogicalSource LS_0, CAENRFIDTag tag) throws Exception {
            try {
                LS_0.ReadTagData_EPC_C1G2(tag, TRIGBANK, ADDR_TRIGGER, (short) 4);
            } catch (Exception ex) {
                throw new Exception(ex);
            }
        }

        /**
         * This function loads the TAGDATA parameters in the DATA area of the tag memory interface.
         */
        public static void interfacemem_writedata(CAENRFIDLogicalSource LS_0, CAENRFIDTag tag, short address,
                                                  short bytes, byte[] data) throws Exception {
            try {
                LS_0.WriteTagData_EPC_C1G2(tag, CMDBANK, (short) (ADDR_DATA + address), bytes, data);
            } catch (Exception ex) {
                throw new Exception(ex);
            }
        }

        /**
         * This function reads the DATA area of the tag memory interface.
         */
        public static byte[] interfacemem_readdata(CAENRFIDLogicalSource LS_0, CAENRFIDTag tag, short address,
                                                   short bytes) throws Exception {
            byte[] data;
            try {
                data = LS_0.ReadTagData_EPC_C1G2(tag, CMDBANK, (short) (ADDR_DATA + address), bytes);
                return data;
            } catch (Exception ex) {
                throw new Exception(ex);
            }
        }

        /**
         * This function reads the REPLY register of the tag memory interface
         */
        public static byte[] interfacemem_readreply(CAENRFIDLogicalSource LS_0, CAENRFIDTag tag) throws Exception {
            byte[] data;
            try {
                data = LS_0.ReadTagData_EPC_C1G2(tag, CMDBANK, ADDR_REPLY, (short) 2);
                return data;
            } catch (Exception ex) {
                throw new Exception(ex);
            }
        }
    }

    /**
     * Reads from RT0013-Tags internal memory
     *
     * @author CAEN
     * @implNote Using CAEN API 5.0.0
     * @see "CAEN Technical Information"
     */
    public byte[] TagReadRegisters(CAENRFIDLogicalSource LS_0, CAENRFIDTag tag, short byteaddress, short numreg) throws Exception {
        int TIME_WAITTAG_CMDREADBASE = 100;
        int TIME_WAITTAG_WRITEPAGE = 7;
        byte idmsg = 0;
        short command;
        short numbytes = (short) (numreg * 2);
        byte reply = INTERFACEMEM.REPLY_NACK;

        //check parameters
        if (byteaddress % 2 != 0) {
            throw new Exception("Access at byte level not allowed");
        }
        if (numbytes > INTERFACEMEM.MAXBYTESIZEDATA) {
            throw new Exception("Invalid size! Too many bytes requested!");
        }

        //check current idmsg value written in reply word and adjust idmsg of next command accordingly
        if (LS_0.ReadTagData_EPC_C1G2(tag, INTERFACEMEM.CMDBANK, INTERFACEMEM.ADDR_REPLY, (short) 2)[0] == idmsg)
            idmsg++;
        command = (short) (idmsg << 8 | INTERFACEMEM.CMD_READ);

        //load command parameters in user memory
        INTERFACEMEM.interfacemem_writeparams(LS_0, tag, command, (short) (byteaddress / 2), (short) (numbytes / 2));
        //trigger tag command reception+execution
        INTERFACEMEM.interfacemem_trigger(LS_0, tag);
        //wait for tag to parse command, execute it, and reply
        Thread.sleep(TIME_WAITTAG_CMDREADBASE + (long) TIME_WAITTAG_WRITEPAGE * (numbytes / 4 + 1));

        //check if tag replied
        byte[] buff = INTERFACEMEM.interfacemem_readreply(LS_0, tag);
        if (buff[0] == idmsg) {
            reply = buff[1];
            if (!((reply == INTERFACEMEM.REPLY_ACK) || (reply == INTERFACEMEM.REPLY_NACK))) {
                throw new Exception("Tag reply time out");
            }
        } else {
            throw new Exception("Tag reply time out");
        }

        //check reply
        if (reply == INTERFACEMEM.REPLY_NACK) {
            throw new Exception("Tag replied NACK");
        }

        //tag replied ACK, now we can read the TAGDATA
        return INTERFACEMEM.interfacemem_readdata(LS_0, tag, (short) 0, numbytes);
    }

    /**
     * Writes on RT0013-Tags internal memory
     *
     * @author CAEN
     * @implNote Using CAEN API 5.0.0
     * @see "CAEN Technical Information"
     */
    public void TagWriteRegisters(CAENRFIDLogicalSource LS_0, CAENRFIDTag tag, short byteaddress, short numreg, byte[] data) throws Exception {
        int TIME_WAITTAG_CMDWRITE = 400;
        byte idmsg = 0;
        short command;
        short numbytes = (short) (numreg * 2);
        byte reply = INTERFACEMEM.REPLY_NACK;

        //check parameters
        if (byteaddress % 2 != 0) {
            throw new Exception("Access at byte level not allowed");
        }
        if (numbytes > INTERFACEMEM.MAXBYTESIZEDATA) {
            throw new Exception("Invalid size! Too many bytes requested!");
        }

        //check current idmsg value written in reply word and adjust idmsg of next command accordingly
        if (LS_0.ReadTagData_EPC_C1G2(tag, INTERFACEMEM.CMDBANK, INTERFACEMEM.ADDR_REPLY, (short) 2)[0] == idmsg)
            idmsg++;
        command = (short) (idmsg << 8 | INTERFACEMEM.CMD_WRITE);

        //load command parameters in user memory
        INTERFACEMEM.interfacemem_writeparams(LS_0, tag, command, (short) (byteaddress / 2), (short) (numbytes / 2));
        //load TAGDATA to be written
        INTERFACEMEM.interfacemem_writedata(LS_0, tag, (short) 0, numbytes, data);
        //trigger tag command reception+execution
        INTERFACEMEM.interfacemem_trigger(LS_0, tag);
        //wait for tag to parse command, execute it, and reply
        Thread.sleep(TIME_WAITTAG_CMDWRITE);

        //check if tag replied
        byte[] buff = INTERFACEMEM.interfacemem_readreply(LS_0, tag);
        if (buff[0] == idmsg) {
            reply = buff[1];
            if (!((reply == INTERFACEMEM.REPLY_ACK) || (reply == INTERFACEMEM.REPLY_NACK))) {
                throw new Exception("Tag reply time out");
            }
        } else {
            throw new Exception("Tag reply time out");
        }

        //check reply
        if (reply == INTERFACEMEM.REPLY_NACK) {
            throw new Exception("Tag replied NACK");
        }
    }

    /**
     * Little usage wrapper for TagReadRegisters()
     * @author Emil Sedlacek / it231503
     * @see INTERFACEMEM
     * @implNote Using CAEN API 5.0.0
     */
    public byte[] readTag(short wordddress, short words2read) {
        byte[] dataToRead = null;
        for (int i = 1; i <= 3; i++) {
            try {
                dataToRead = TagReadRegisters(myReader.GetSources()[0], tag, (short) (wordddress * 2), words2read);  // Wordaddress from RT0013-DOC to byteadress
                break;
            } catch (Exception e) {
                System.err.println("Error in general read operation! Try #" + i);
                if (i == 3)
                    throw new RuntimeException("Critical: Giving up reading from tag.");
            }
        }

        return dataToRead;
    }

    /**
     * Little usage wrapper for TagWriteRegisters()
     * @author Emil Sedlacek / it231503
     * @see INTERFACEMEM
     * @implNote Using CAEN API 5.0.0
     */
    public void writeTag(short wordaddress, short[] dataToWrite) {
        byte[] temp = shortsToBytes(dataToWrite);
        if (temp.length % 2 != 0) {
            System.err.println("Error: Datalength to write is odd.");
            return;
        }
        for (int i = 1; i <= 3; i++) {
            try {
                TagWriteRegisters(myReader.GetSources()[0], tag, (short) (wordaddress * 2), (short) dataToWrite.length, temp);
                break;
            } catch (Exception e) {
                System.err.println("Error in write operation! Attempt #" + i);
                if (i == 3) {
                    throw new RuntimeException("Critical: Giving up writing to tag.");
                }
            }
        }
    }
}
