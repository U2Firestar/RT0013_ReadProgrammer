package com.fhstp.it231503.caen.rfid;

/**
 * Interface specifications for QLOG CAEN RT0013 RFID TAG.
 * @author Emil Sedlacek / it231503
 * */
 public interface RT0013 {
    //////////////////////// RT00013 INFORMATION TYPES
    /**
     * RT0013 Bin Enabler Types.
     * */
     enum BIN_EN_TYPES {COUNTER, SAMPLE_STORE, TIME_STORE}
    /**
     * RT0013 Sensor Types.
     * */
    enum BIN_SENSOR_TYPES {TEMPERATURE, HUMIDITY}
    /**
     * RT0013 Revision Meta Information Types.
     * */
    enum REV_TYPES {HW, FW}
    /**
     * RT0013 Control register bits.
     * */
    enum BITS_CTRL {RST, LE, DE, RFSL}
    /**
     * RT0013 Status register bits.
     * */
    enum BITS_STATUS {BAT_LS, BAT_MS, MEMFULL_T, MEMFULL_H, BIN_ALRM_T, BIN_ALRM_H, ETA_ALRM}

    //////////////////////// RT00013 DOC REGISTER
     short REG_START = 0x0;
     short REG_END = 0x1089;

     short REG_FW_REVISION = 0x08;
     short REG_HW_REVISION = 0x09;

     short REG_CONTROL = 0x0A;
    //CONTROL register bit positions
     short BITPOS_CTRL_RST = 0;
     short BITPOS_CTRL_LE = 1;
     short BITPOS_CTRL_DE = 2;
     short BITPOS_CTRL_RFSL = 3;

     short REG_SAMPLING_DELAY = 0x0B;
     short REG_INIT_DATE_L = 0x0C;
     short REG_INIT_DATE_H = 0x0D;
     short REG_ETA_L = 0x0E;
     short REG_ETA_H = 0x0F;

     short REG_BIN_ENA_COUNTER = 0x10;
     short REG_BIN_ENA_SAMPLE_STORE = 0x11;
     short REG_BIN_ENA_TIME_STORE = 0x12;
    //BIN_ENABLE_COUNTER register bits
    //BIN_ENA_SAMPLE_STORE register bits
    //BIN_ENA_TIME_STORE register bits
     short BITPOS_BIN00_T_EN = 0;
     short BITPOS_BIN01_T_EN = 1;
     short BITPOS_BIN02_T_EN = 2;
     short BITPOS_BIN03_T_EN = 3;
     short BITPOS_BIN04_T_EN = 4;
     short BITPOS_BIN05_T_EN = 5;
     short BITPOS_BIN00_H_EN = 6;
     short BITPOS_BIN01_H_EN = 7;
     short BITPOS_BIN02_H_EN = 8;
     short BITPOS_BIN03_H_EN = 9;
     short BITPOS_BIN04_H_EN = 10;
     short BITPOS_BIN05_H_EN = 11;

     short REG_BIN_HLIMIT_T_0 = 0x13;
     short REG_BIN_HLIMIT_T_1 = 0x14;
     short REG_BIN_HLIMIT_T_2 = 0x15;
     short REG_BIN_HLIMIT_T_3 = 0x16;
     short REG_BIN_HLIMIT_T_4 = 0x17;
     short REG_BIN_HLIMIT_T_5 = 0x18;

     short REG_BIN_HLIMIT_H_0 = 0x19;
     short REG_BIN_HLIMIT_H_1 = 0x1A;
     short REG_BIN_HLIMIT_H_2 = 0x1B;
     short REG_BIN_HLIMIT_H_3 = 0x1C;
     short REG_BIN_HLIMIT_H_4 = 0x1D;
     short REG_BIN_HLIMIT_H_5 = 0x1E;

     short REG_BIN_SAMPLETIME_T_0 = 0x23;
     short REG_BIN_SAMPLETIME_T_1 = 0x24;
     short REG_BIN_SAMPLETIME_T_2 = 0x25;
     short REG_BIN_SAMPLETIME_T_3 = 0x26;
     short REG_BIN_SAMPLETIME_T_4 = 0x27;
     short REG_BIN_SAMPLETIME_T_5 = 0x28;

     short REG_BIN_SAMPLETIME_H_0 = 0x29;
     short REG_BIN_SAMPLETIME_H_1 = 0x2A;
     short REG_BIN_SAMPLETIME_H_2 = 0x2B;
     short REG_BIN_SAMPLETIME_H_3 = 0x2C;
     short REG_BIN_SAMPLETIME_H_4 = 0x2D;
     short REG_BIN_SAMPLETIME_H_5 = 0x2E;

     short REG_BIN_THRESHOLD_T_0 = 0x33;
     short REG_BIN_THRESHOLD_T_1 = 0x34;
     short REG_BIN_THRESHOLD_T_2 = 0x35;
     short REG_BIN_THRESHOLD_T_3 = 0x36;
     short REG_BIN_THRESHOLD_T_4 = 0x37;
     short REG_BIN_THRESHOLD_T_5 = 0x38;

     short REG_BIN_THRESHOLD_H_0 = 0x39;
     short REG_BIN_THRESHOLD_H_1 = 0x3A;
     short REG_BIN_THRESHOLD_H_2 = 0x3B;
     short REG_BIN_THRESHOLD_H_3 = 0x3C;
     short REG_BIN_THRESHOLD_H_4 = 0x3D;
     short REG_BIN_THRESHOLD_H_5 = 0x3E;

     short REG_STATUS = 0x51;
    //STATUS register bits
     short BITPOS_BAT_LS = 0;
     short BITPOS_BAT_MS = 1;
     short BITPOS_MEMFULL_T = 2;
     short BITPOS_MEMFULL_H = 10;
     short BITPOS_BIN_ALRM_T = 4;
     short BITPOS_BIN_ALRM_H = 11;
     short BITPOS_ETA_ALRM = 3;

     short REG_BIN_ALARM = 0x55;
    //BIN_ALARM register bits
     short BITPOS_BIN00_EN = 0;
     short BITPOS_BIN01_EN = 1;
     short BITPOS_BIN02_EN = 2;
     short BITPOS_BIN03_EN = 3;
     short BITPOS_BIN04_EN = 4;
     short BITPOS_BIN05_EN = 5;
     short BITPOS_BIN06_EN = 6;
     short BITPOS_BIN07_EN = 7;
     short BITPOS_BIN08_EN = 8;
     short BITPOS_BIN09_EN = 9;
     short BITPOS_BIN10_EN = 10;
     short BITPOS_BIN11_EN = 11;
     short BITPOS_BIN12_EN = 12;
     short BITPOS_BIN13_EN = 13;
     short BITPOS_BIN14_EN = 14;
     short BITPOS_BIN15_EN = 15;

     short REG_BIN_COUNTER_T_0 = 0x56;
     short REG_BIN_COUNTER_T_1 = 0x57;
     short REG_BIN_COUNTER_T_2 = 0x58;
     short REG_BIN_COUNTER_T_3 = 0x59;
     short REG_BIN_COUNTER_T_4 = 0x5A;
     short REG_BIN_COUNTER_T_5 = 0x5B;

     short REG_BIN_COUNTER_H_0 = 0x5C;
     short REG_BIN_COUNTER_H_1 = 0x5D;
     short REG_BIN_COUNTER_H_2 = 0x5E;
     short REG_BIN_COUNTER_H_3 = 0x5F;
     short REG_BIN_COUNTER_H_4 = 0x60;
     short REG_BIN_COUNTER_H_5 = 0x61;

     short REG_LAST_SAMPLE_VALUE_T = 0x62;
     short REG_LAST_SAMPLE_VALUE_H = 0x63;

     short REG_SAMPLES_NUM_T = 0x64;
     short REG_SAMPLES_NUM_H = 0x65;

     short REG_SHIPPING_DATE_L = 0x6A;
     short REG_SHIPPING_DATE_H = 0x6B;

     short REG_STOP_DATE_L = 0x6C;
     short REG_STOP_DATE_H = 0x6D;

     short REG_USER_AREA_START = 0x6E;
     short REG_USER_AREA_END = 0x89;

     short REG_LOG_AREA_T_START = 0x8A;
     short REG_LOG_AREA_T_END = 0x889;

     short REG_LOG_AREA_H_START = 0x88A;
     short REG_LOG_AREA_H_END = 0x1089;
}
