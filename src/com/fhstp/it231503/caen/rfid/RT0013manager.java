package com.fhstp.it231503.caen.rfid;

import com.fhstp.it231503.caen.rfid.RT0013.BIN_SENSOR_TYPES;
import com.fhstp.it231503.caen.rfid.RT0013.BITS_CTRL;
import com.fhstp.it231503.caen.rfid.RT0013.BITS_STATUS;
import com.fhstp.it231503.caen.rfid.RT0013.REV_TYPES;
import com.fhstp.it231503.caen.util.binSetting;
import com.fhstp.it231503.caen.util.measurmentPoint;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.fhstp.it231503.caen.rfid.RT0013rain.*;
import static com.fhstp.it231503.caen.util.Helper.*;

/**
 * High level processing class for QLOG CAEN RT0013 RFID TAG.
 *
 * @author Emil Sedlacek / it231503
 * @author CAEN
 * @implNote Using CAEN API 5.0.0
 * @see "CAEN Technical Information"
 */
public class RT0013manager {
    /**
     * Tag accessor variable.
     */
    public RT0013rain myRT0013rain = new RT0013rain();
    /**
     * Tag register buffer for speedup purposes. Stores wordregisters and their short values
     */
    private final Map<Short, Short> buffer = new ConcurrentHashMap<>();

    /**
     * Fetches all registers and updates the buffer.
     *
     * @implNote Thread-safe
     */
    public synchronized void bufferFetchRegisters() {
        short words2read;
        short[] datawordsToRead;
        for (short wordaddress = REG_START; wordaddress <= REG_END; wordaddress += 100) {
            words2read = (wordaddress + 100 > REG_END) ? (short) (REG_END - wordaddress + 1) : (short) 100;
            datawordsToRead = bytesToShorts(myRT0013rain.readTag(wordaddress, words2read)); // Reading tag and converting bytes to shorts
            for (short x = 0; x < words2read; x++) {
                buffer.put((short) (wordaddress + x), datawordsToRead[x]);
                //System.out.println("Buffering register 0x" + shortToHexString((short) (wordaddress + x)) + ": 0x" + shortToHexString(datawordsToRead[x]));
                progressPercentage(wordaddress + x, REG_END);
            }
        }
    }

    /**
     * Resets buffer to free up memory. Also useful in case of suspicion that tag will have different values.
     *
     * @implNote Thread-safe
     */
    public synchronized void bufferReset() {
        buffer.clear();
    }

    /**
     * Retrieves the value of a register from the buffer
     *
     * @param wordaddress Address of tag register
     * @return Buffered or retrieved register value
     * @implNote Thread-safe
     */
    public synchronized short bufferGetValue(short wordaddress) {
        Short value = buffer.get(wordaddress);
        if (value == null) {
            value = bytesToShort(myRT0013rain.readTag(wordaddress, (short) 1));
            buffer.put(wordaddress, value);
        }
        return value;
    }

    /**
     * Updates a specific register in the tag and buffer. Checks whether register was
     *
     * @param wordaddress Address of tag register
     * @param newValue    Value to write to buffer and tag.
     * @implNote Thread-safe
     */
    public synchronized void bufferUpdateValue(short wordaddress, short newValue) {
        myRT0013rain.writeTag(wordaddress, new short[]{newValue});
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        short currentState = bytesToShort(myRT0013rain.readTag(wordaddress, (short) 1));
        if (currentState != newValue)
            System.err.println("Error in updating tag register: 0x" + shortToHexString(wordaddress) + " to 0x" + shortToHexString(newValue) + " - still is: 0x" + shortToHexString(currentState));

        buffer.put(wordaddress, currentState); // Update Buffer
    }

    /**
     * Loads bin configuration from buffer (=tag) and returns readable data structure.
     *
     * @param sensor Type of Sensor
     * @return Unchecked array of binSettings.
     */
    public List<binSetting> retrieveBinsConfiguration(BIN_SENSOR_TYPES sensor) {
        List<binSetting> settings = new ArrayList<>();
        for (short binIndex = 0; binIndex <= 5; binIndex++) {
            progressPercentage(binIndex, 5);
            settings.add(new binSetting(
                    getBinOptionHLIMIT(binIndex, sensor),
                    getBinOptionTHRESHOLD(binIndex, sensor),
                    getBinEnX(binIndex, BIN_EN_TYPES.SAMPLE_STORE, sensor),
                    getBinEnX(binIndex, BIN_EN_TYPES.TIME_STORE, sensor),
                    getBinOptionSAMPLETIME(binIndex, sensor)
            ));
        }
        return settings;
    }

    /**
     * Stores bin configuration to buffer (=tag).
     *
     * @param sensor   Type of Sensor
     * @param settings Sorted and checked array of binSettings. Settings being empty is OK!
     * @throws IllegalFormatCodePointException If wrong sensor is given.
     */
    public void configureBins(List<binSetting> settings, BIN_SENSOR_TYPES sensor) {

        if (settings != null && settings.isEmpty()) {
            System.err.print("No bins configured for ");
            if (sensor == BIN_SENSOR_TYPES.TEMPERATURE)
                System.err.println("TEMPERATURE!");
            else if (sensor == BIN_SENSOR_TYPES.HUMIDITY) {
                System.err.println("HUMIDITY!");
            } else
                throw new IllegalArgumentException("Wrong sensor given");
        }

        // RESET or WRITE
        for (short i = 0; i <= 5; i++) {
            progressPercentage(i, 5);
            //System.out.println("Writing bin #" + i + " config to TAG!");
            if (settings != null && !settings.isEmpty() && settings.size() >= i + 1) {
                setBinOptionHLIMIT(i, sensor, settings.get(i).getHighLimit());
                setBinEnX(i, BIN_EN_TYPES.COUNTER, sensor, true); // Needed for log data
                setBinOptionTHRESHOLD(i, sensor, settings.get(i).getThreshold());
                setBinEnX(i, BIN_EN_TYPES.SAMPLE_STORE, sensor, settings.get(i).isStoreSamples());
                setBinEnX(i, BIN_EN_TYPES.TIME_STORE, sensor, settings.get(i).isStoreTimes());
                setBinOptionSAMPLETIME(i, sensor, settings.get(i).getSamplingInterval(), false);
            } else {
                setBinEnX(i, BIN_EN_TYPES.COUNTER, sensor, i == 0);
                setBinEnX(i, BIN_EN_TYPES.SAMPLE_STORE, sensor, i == 0);
                setBinEnX(i, BIN_EN_TYPES.TIME_STORE, sensor, false);
                setBinOptionSAMPLETIME(i, sensor, (short) 0x1E, false);
                setBinOptionTHRESHOLD(i, sensor, (short) 0xFFFF);
                setBinOptionHLIMIT(i, BIN_SENSOR_TYPES.TEMPERATURE, (i == 0) ? 0x08C0 : 0x0);
                setBinOptionHLIMIT(i, BIN_SENSOR_TYPES.HUMIDITY, (i == 0) ? 0x0C80 : 0x0);
            }
        }
    }


    /////////////////////// DATA MANIPULATION AND DISPLAY

    /**
     * Gets Tag ID from CAEN API Wrapper.
     *
     * @return Tag ID
     */
    public byte[] getTagID() {
        return myRT0013rain.getTag().GetId();
    }

    /**
     * Gets Tag ID from CAEN API Wrapper.
     *
     * @param preamble Whether header shall be added
     * @return Internally defined Tag ID
     */
    public String getTagID(boolean preamble) {
        String output = "";
        if (preamble)
            output += "Tag ID: ";
        return output + bytesToHexstring(getTagID());
    }

    /**
     * Gets Tag Revision information from tag.
     *
     * @param revisiontype Whether HardWare or FirmWare according to enum
     * @return Stored version
     * @throws IllegalFormatCodePointException If revisiontype is wrong
     */
    public float getTagXRev(REV_TYPES revisiontype) {
        short wordaddress;
        switch (revisiontype) {
            case FW -> wordaddress = REG_FW_REVISION;
            case HW -> wordaddress = REG_HW_REVISION;
            case null, default -> throw new IllegalArgumentException("Unknown revision type: " + revisiontype);
        }
        byte[] dataToRead = shortToBytes(bufferGetValue(wordaddress));
        return (float) dataToRead[0] + (float) dataToRead[1] / 10;
    }

    /**
     * Gets Tag Revision information from tag.
     *
     * @param preamble     Whether header shall be added
     * @param revisiontype Whether HardWare or FirmWare according to enum
     * @return Stored version
     */
    public String getTagXRev(REV_TYPES revisiontype, boolean preamble) {
        if (revisiontype != REV_TYPES.FW && revisiontype != REV_TYPES.HW)
            return null;

        String output = "";
        if (preamble) {
            if (revisiontype == REV_TYPES.FW)
                output += "FW";
            if (revisiontype == REV_TYPES.HW)
                output += "HW";
            output += "-Rev: ";
        }
        return output + String.format("v%1.1f", getTagXRev(revisiontype)).replace(',', '.');
    }

    /**
     * Gets Tag Control information from tag.
     *
     * @param bitName Bitposition in Control Register according to enum
     * @return Value of requested bit
     * @throws IllegalArgumentException If bit does not exist.
     */
    public boolean getTagControl(BITS_CTRL bitName) {
        short bit = switch (bitName) {
            case RST -> BITPOS_CTRL_RST;
            case LE -> BITPOS_CTRL_LE;
            case DE -> BITPOS_CTRL_DE;
            case RFSL -> BITPOS_CTRL_RFSL;
            case null, default -> throw new IllegalArgumentException("Unknown bit type: " + bitName);
        };
        return getBit(bufferGetValue(REG_CONTROL), bit);
    }

    /**
     * Gets Tag Control information from tag.
     *
     * @param bit      Bitposition in Control Register according to enum
     * @param preamble Whether header shall be added
     * @return Value of requested bit
     * @throws IllegalArgumentException If bit does not exist.
     */
    public String getTagControl(BITS_CTRL bit, boolean preamble) {
        String output = "";
        if (preamble)
            output += "Control ";
        output += switch (bit) {
            case RST -> "Reset-Bit: ";
            case LE -> "Logging Enable-Bit: ";
            case DE -> "Delay Enable-Bit: ";
            case RFSL -> "RF_Sensitivity_Level-Bit: ";
            case null, default -> throw new IllegalArgumentException("Unknown bit type: " + bit);
        };
        return output + getTagControl(bit);
    }

    /**
     * Sets Tag Control information on tag.
     *
     * @param bitName Bitposition in Control Register according to enum
     * @param value   Bool value to write.
     * @throws IllegalArgumentException If bit does not exist.
     */
    public void setTagControl(BITS_CTRL bitName, boolean value) {
        short bit = switch (bitName) {
            case RST -> BITPOS_CTRL_RST;
            case LE -> BITPOS_CTRL_LE;
            case DE -> BITPOS_CTRL_DE;
            case RFSL -> BITPOS_CTRL_RFSL;
            case null, default -> throw new IllegalArgumentException("Unknown bit type: " + bitName);
        };
        bufferUpdateValue(REG_CONTROL, (short) setBit(bufferGetValue(REG_CONTROL), bit, value));
    }

    /**
     * Initiates and waits for reset on tag.
     *
     * @return Success of operation
     * @param preamble Displays warnings
     * @throws RuntimeException If sleep doesnt work.
     */
    public boolean resetTag(boolean preamble) {
        if (preamble)
            System.out.println("Performing reset... (Checking every 10sec; Timeout is 600sec)");
        final int timeoutSec = 600;
        Instant startTime = Instant.now();
        boolean result = false;

        setTagControl(BITS_CTRL.RST, true);
        do {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            bufferReset();
            if (!getTagControl(BITS_CTRL.RST)) {
                result = true;
                break;
            }
        } while (!startTime.isAfter(startTime.plusSeconds(timeoutSec)));
        return result;
    }

    /**
     * Gets Sampling delay (after Logging is enabled) from tag.
     *
     * @return Time in seconds
     */
    public short getSamplingDelay() {
        return bufferGetValue(REG_SAMPLING_DELAY);
    }

    /**
     * Gets Sampling delay (after Logging is enabled) from tag.
     *
     * @param preamble Whether header shall be added
     * @param unit     Whether unit shall be added
     * @return Time in seconds
     */
    public String getSamplingDelay(boolean preamble, boolean unit) {
        String output = "";
        if (preamble)
            output += "Sampling Delay: ";
        output += Short.toString(getSamplingDelay());
        if (unit)
            output += " sec";
        return output;
    }

    /**
     * Gets Sampling delay (after Logging is enabled) from tag.
     *
     * @param delay    Value to write in seconds
     * @param preamble Whether warnings shall be added
     */
    public void setSamplingDelay(short delay, boolean preamble) {
        if (delay > 0 && delay % 5 != 0) {
            if (preamble)
                System.out.println("Warning: The minimum sampling unit is 5 seconds. Sampling delay must be multiple of 5, it will be approximated to the next multiple of 5");
            delay = (short) (delay + delay % 5);
        }
        bufferUpdateValue(REG_SAMPLING_DELAY, delay);
    }

    /**
     * Gets Date of Initialization from tag.
     *
     * @return DateTime of init
     */
    public Date getINITDate() {
        return shortsToDate(new short[]{
                bufferGetValue(REG_INIT_DATE_H),
                bufferGetValue(REG_INIT_DATE_L)
        });
    }

    /**
     * Gets Date of Initialization from tag.
     *
     * @param preamble Whether header shall be added
     * @return Formatted DateTime of init
     */
    public String getINITDate(boolean preamble) {
        String output = "";
        if (preamble)
            output += "Init Date: ";
        //output += getINITDate().toString();
        output += formatTimestamp(LocalDateTime.ofInstant(getINITDate().toInstant(), ZoneOffset.ofHours(1))); // TODO: OK?
        return output;
    }

    /**
     * Sets Date of Initialization on tag.
     *
     * @param initDate Whether header shall be added
     */
    public void setINITDate(Date initDate) {
        short[] dateWords = dateToShorts(initDate);
        bufferUpdateValue(REG_INIT_DATE_L, dateWords[1]);
        bufferUpdateValue(REG_INIT_DATE_H, dateWords[0]);
    }

    /**
     * Gets ETA (after Logging is enabled) from tag.
     *
     * @return Time in seconds
     */
    public int getETA() {
        return shortsToInt(new short[]{
                bufferGetValue(REG_ETA_H),
                bufferGetValue(REG_ETA_L)
        });
    }

    /**
     * Gets ETA (after Logging is enabled) from tag.
     *
     * @param preamble Whether header shall be added
     * @param unit     Whether unit shall be added
     */
    public String getETA(boolean preamble, boolean unit) throws Exception {
        String output = "";
        if (preamble)
            output += "ETA: ";
        output += Integer.toString(getETA());
        if (unit)
            output += " sec";
        output += " (" + getShippingDate().toInstant().plusSeconds(getETA()).toString() + ")";  // print goal date
        return output;
    }

    /**
     * Sets ETA (after Logging is enabled) on tag.
     *
     * @param eta Time in seconds
     */
    public void setETA(int eta) {
        short[] etaWords = intToShorts(eta);
        bufferUpdateValue(REG_ETA_L, etaWords[1]);
        bufferUpdateValue(REG_ETA_H, etaWords[0]);
    }

    /**
     * Gets bit status of given enabling register, sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param bin        Bin type according to enum
     * @param sensorType Type of Sensor according to enum
     * @return Stored value
     * @throws IllegalArgumentException If bin does not exist.
     */
    public boolean getBinEnX(short binNum, BIN_EN_TYPES bin, BIN_SENSOR_TYPES sensorType) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        short wordaddress, offset;
        switch (sensorType) {
            case TEMPERATURE -> offset = BITPOS_BIN00_T_EN;
            case HUMIDITY -> offset = BITPOS_BIN00_H_EN;
            case null, default -> throw new IllegalArgumentException("Unknown bin sensorType: " + sensorType);
        }
        switch (bin) {
            case COUNTER -> wordaddress = REG_BIN_ENA_COUNTER;
            case TIME_STORE -> wordaddress = REG_BIN_ENA_TIME_STORE;
            case SAMPLE_STORE -> wordaddress = REG_BIN_ENA_SAMPLE_STORE;
            case null, default -> throw new IllegalArgumentException("Unknown bin type: " + bin);
        }
        return getBit(bufferGetValue(wordaddress), offset + binNum);
    }

    /**
     * Gets bit status of given enabling register, sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param bin        Bin type according to enum
     * @param sensorType Type of Sensor according to enum
     * @param preamble   Whether header shall be added
     * @return Stored value
     * @throws IllegalArgumentException If bin does not exist.
     */
    public String getBinEnX(short binNum, BIN_EN_TYPES bin, BIN_SENSOR_TYPES sensorType, boolean preamble) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        String output = "";
        if (preamble) {
            output += "BIN ENABLE ";
            output += switch (bin) {
                case COUNTER -> "COUNTER ";
                case TIME_STORE -> "TIME_STORE ";
                case SAMPLE_STORE -> "SAMPLE_STORE ";
            };
            output += switch (sensorType) {
                case TEMPERATURE -> "(TEMPERATURE) ";
                case HUMIDITY -> "(HUMIDITY) ";
            };
            output += "#" + binNum + ": ";
        }
        output += Boolean.toString(getBinEnX(binNum, bin, sensorType));
        return output;
    }

    /**
     * Sets bit status of given enabling register, sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param bin        Bin type according to enum
     * @param sensorType Type of Sensor according to enum
     * @param enable     Value to store
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public void setBinEnX(short binNum, BIN_EN_TYPES bin, BIN_SENSOR_TYPES sensorType, boolean enable) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        short wordaddress = switch (bin) {
            case COUNTER -> REG_BIN_ENA_COUNTER;
            case TIME_STORE -> REG_BIN_ENA_TIME_STORE;
            case SAMPLE_STORE -> REG_BIN_ENA_SAMPLE_STORE;
            case null, default -> throw new IllegalArgumentException("Unknown bin: " + bin);
        };
        short offset = switch (sensorType) {
            case TEMPERATURE -> BITPOS_BIN00_T_EN;
            case HUMIDITY -> BITPOS_BIN00_H_EN;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };

        // Retrieve the current data, modify it, and write it back
        bufferUpdateValue(wordaddress, (short) setBit(bufferGetValue(wordaddress), (short) (offset + binNum), enable));
    }

    /**
     * Gets higher limit value in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @return Converted, human-readable value
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public float getBinOptionHLIMIT(short binNum, BIN_SENSOR_TYPES sensorType) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);
        short wordaddress = switch (sensorType) {
            case TEMPERATURE -> REG_BIN_HLIMIT_T_0;
            case HUMIDITY -> REG_BIN_HLIMIT_H_0;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };
        return fixedpointToFloat(sensorType, bufferGetValue((short) (wordaddress + binNum)));
    }

    /**
     * Gets higher limit value in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @param preamble   Whether header shall be added
     * @param unit       Whether unit shall be added
     * @return Converted, human-readable value
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public String getBinOptionHLIMIT(short binNum, BIN_SENSOR_TYPES sensorType, boolean preamble, boolean unit) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        String output = "";
        if (preamble) {
            output += "BIN HIGHER LIMIT ";
            output += switch (sensorType) {
                case TEMPERATURE -> "(TEMPERATURE) ";
                case HUMIDITY -> "(HUMIDITY) ";
                case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
            };
            output += "#" + binNum + ": ";
        }
        output += Float.toString(getBinOptionHLIMIT(binNum, sensorType));
        if (unit) {
            output += switch (sensorType) {
                case TEMPERATURE -> " °C";
                case HUMIDITY -> " %";
            };
        }
        return output;
    }

    /**
     * Sets higher limit value in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @param limit      Human-readable value in seconds
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public void setBinOptionHLIMIT(short binNum, BIN_SENSOR_TYPES sensorType, float limit) {
        if (binNum < 0 || binNum > 5)
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        short offset = switch (sensorType) {
            case TEMPERATURE -> REG_BIN_HLIMIT_T_0;
            case HUMIDITY -> REG_BIN_HLIMIT_H_0;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };

        bufferUpdateValue((short) (offset + binNum), floatToFixedpoint(sensorType, limit));
    }

    /**
     * Gets sample time in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @return Converted, human-readable value in seconds
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public short getBinOptionSAMPLETIME(short binNum, BIN_SENSOR_TYPES sensorType) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        short offset = switch (sensorType) {
            case TEMPERATURE -> REG_BIN_SAMPLETIME_T_0;
            case HUMIDITY -> REG_BIN_SAMPLETIME_H_0;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };
        return bufferGetValue((short) (offset + binNum));
    }

    /**
     * Gets sample time in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @param preamble   Whether header shall be added
     * @param unit       Whether unit shall be added
     * @return Converted, human-readable value in seconds
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public String getBinOptionSAMPLETIME(short binNum, BIN_SENSOR_TYPES sensorType, boolean preamble, boolean unit) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        String output = "";
        if (preamble) {
            output += "BIN SAMPLE TIME ";
            output += switch (sensorType) {
                case TEMPERATURE -> "(TEMPERATURE) ";
                case HUMIDITY -> "(HUMIDITY) ";
                case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
            };
            output += "#" + binNum + ": ";
        }
        output += Short.toString(getBinOptionSAMPLETIME(binNum, sensorType));
        if (unit)
            output += " sec";
        return output;
    }

    /**
     * Gets sample time in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @param sampleTime Human-readable value
     * @param info       Whether to print warnings.
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public void setBinOptionSAMPLETIME(short binNum, BIN_SENSOR_TYPES sensorType, short sampleTime, boolean info) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        if (sampleTime < 5) {
            if (info)
                System.out.println("Warning: The minimum sampling unit is 5 seconds. Setting to 5sec.");
            sampleTime = 5;
        }
        if (sampleTime % 5 != 0) {
            if (info)
                System.out.println("Warning: Sampling delay must be multiple of 5, it will be approximated to the next multiple of 5");
            sampleTime = (short) (sampleTime + sampleTime % 5);
        }

        short offset = switch (sensorType) {
            case TEMPERATURE -> REG_BIN_SAMPLETIME_T_0;
            case HUMIDITY -> REG_BIN_SAMPLETIME_H_0;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };

        bufferUpdateValue((short) (offset + binNum), sampleTime);
    }

    /**
     * Gets alarm-threshold (count of values) in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @return Set counts till alarm
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public short getBinOptionTHRESHOLD(short binNum, BIN_SENSOR_TYPES sensorType) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        short offset = switch (sensorType) {
            case TEMPERATURE -> REG_BIN_THRESHOLD_T_0;
            case HUMIDITY -> REG_BIN_THRESHOLD_H_0;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };

        return bufferGetValue((short) (offset + binNum));
    }

    /**
     * Gets alarm-threshold (count of values) in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @param preamble   Whether header shall be added
     * @return Set counts till alarm.
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public String getBinOptionTHRESHOLD(short binNum, BIN_SENSOR_TYPES sensorType, boolean preamble) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        String output = "";
        if (preamble) {
            output += "BIN THRESHOLD (COUNTS) ";
            output += switch (sensorType) {
                case TEMPERATURE -> "(TEMPERATURE) ";
                case HUMIDITY -> "(HUMIDITY) ";
                case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
            };
            output += "#" + binNum + ": ";
        }
        output += Short.toString(getBinOptionTHRESHOLD(binNum, sensorType));
        return output;
    }

    /**
     * Sets alarm-threshold (count of values) in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @param threshold  Set counts till alarm.
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public void setBinOptionTHRESHOLD(short binNum, BIN_SENSOR_TYPES sensorType, short threshold) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        short offset = switch (sensorType) {
            case TEMPERATURE -> REG_BIN_THRESHOLD_T_0;
            case HUMIDITY -> REG_BIN_THRESHOLD_H_0;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };

        bufferUpdateValue((short) (offset + binNum), threshold);
    }

    /**
     * Gets Tag Status information from tag.
     *
     * @param bitName Bitposition in Status Register according to enum
     * @return Value of requested bit
     * @throws IllegalArgumentException If bit does not exist.
     */
    public boolean getTagStatus(BITS_STATUS bitName) {
        short dataToRead = bufferGetValue(REG_STATUS);
        short bit = switch (bitName) {
            case BAT_LS -> BITPOS_BAT_LS;
            case BAT_MS -> BITPOS_BAT_MS;
            case BIN_ALRM_T -> BITPOS_BIN_ALRM_T;
            case BIN_ALRM_H -> BITPOS_BIN_ALRM_H;
            case ETA_ALRM -> BITPOS_ETA_ALRM;
            case MEMFULL_T -> BITPOS_MEMFULL_T;
            case MEMFULL_H -> BITPOS_MEMFULL_H;
            case null, default -> throw new IllegalStateException("Unexpected value: " + bitName);
        };
        return getBit(dataToRead, bit);
    }

    /**
     * Gets Tag Status information from tag.
     *
     * @param bitName  Bitposition in Status Register according to enum
     * @param preamble Whether header shall be added
     * @return Value of requested bit
     * @throws IllegalArgumentException If bit does not exist.
     */
    public String getTagStatus(BITS_STATUS bitName, boolean preamble) {
        String output = "";
        if (preamble) {
            output += "Status ";
            output += switch (bitName) {
                case BAT_LS -> "Battery_LS-Bit: ";
                case BAT_MS -> "Battery_MS-Bit: ";
                case BIN_ALRM_T -> "Threshold Alarm (TEMPERATURE): ";
                case BIN_ALRM_H -> "Threshold Alarm (HUMIDITY): ";
                case MEMFULL_T -> "Memfull Alarm (TEMPERATURE): ";
                case MEMFULL_H -> "Memfull Alarm (HUMIDITY): ";
                case ETA_ALRM -> "ETA Overstep Alarm: ";
                case null, default -> throw new IllegalStateException("Unexpected value: " + bitName);
            };
        }
        return output + Boolean.toString(getTagStatus(bitName));
    }

    /**
     * Gets Tag Battery information from tag.
     *
     * @param preamble Whether header shall be added
     * @return Translated battery state
     */
    public String getBATStatus(boolean preamble) {
        String output = "";
        boolean BAT_LS = getTagStatus(BITS_STATUS.BAT_LS);
        boolean BAT_MS = getTagStatus(BITS_STATUS.BAT_MS);

        if (preamble)
            output += "Status Battery: ";

        int tmp = ((BAT_MS) ? 2 : 0) + ((BAT_LS) ? 1 : 0);
        if (tmp >= 3) {
            output += "Full";
        } else if (tmp == 2) {
            output += "Normal";
        } else if (tmp == 1) {
            output += "Low";
        } else {
            output += "Empty (no logging)";
        }
        return output;
    }

    /**
     * Gets alarm bit (according to set thresholds) in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @return Whether alarm is triggered
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public boolean getTagAlarm(short binNum, BIN_SENSOR_TYPES sensorType) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        short offset = switch (sensorType) {
            case TEMPERATURE -> 0;
            case HUMIDITY -> 6;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };

        return getBit(bufferGetValue(REG_BIN_ALARM), binNum + offset);
    }

    /**
     * Gets alarm bit (according to set thresholds) in register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @param preamble   Whether header shall be added
     * @return Whether alarm is triggered
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public String getTagAlarm(short binNum, BIN_SENSOR_TYPES sensorType, boolean preamble) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        String output = "";
        if (preamble) {
            output += "Bin Alarm bit ";
            output += switch (sensorType) {
                case TEMPERATURE -> "(TEMPERATURE) ";
                case HUMIDITY -> "(HUMIDITY) ";
                case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
            };
            output += "#" + binNum + ": ";
        }
        return output + Boolean.toString(getTagAlarm(binNum, sensorType));
    }

    /**
     * Gets count of values stored in a bin, read from register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @return Whether alarm is triggered
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public short getBinCounter(short binNum, BIN_SENSOR_TYPES sensorType) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        short offset = switch (sensorType) {
            case TEMPERATURE -> REG_BIN_COUNTER_T_0;
            case HUMIDITY -> REG_BIN_COUNTER_H_0;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };

        return bufferGetValue((short) (offset + binNum));
    }

    /**
     * Gets count of values stored in a bin, read from register by given sensor and bin number.
     *
     * @param binNum     Number of bin (0...5)
     * @param sensorType Type of Sensor according to enum
     * @param preamble   Whether header shall be added
     * @return Whether alarm is triggered
     * @throws IllegalArgumentException If bin or sensor does not exist.
     */
    public String getBinCounter(short binNum, BIN_SENSOR_TYPES sensorType, boolean preamble) {
        if (!(binNum >= 0 && binNum <= 5))
            throw new IllegalArgumentException("Invalid bin number: " + binNum);

        String output = "";
        if (preamble) {
            output += "BIN COUNTS ";
            output += switch (sensorType) {
                case TEMPERATURE -> "(TEMPERATURE) ";
                case HUMIDITY -> "(HUMIDITY) ";
                case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
            };
            output += "#" + binNum + ": ";
        }
        output += Short.toString(getBinCounter(binNum, sensorType));
        return output;
    }

    /**
     * Gets last measurement by given sensor.
     *
     * @param sensorType Type of Sensor according to enum
     * @return Converted, human-readable value
     * @throws IllegalArgumentException If sensor does not exist.
     */
    public float getLastSample(BIN_SENSOR_TYPES sensorType) {
        short offset = switch (sensorType) {
            case TEMPERATURE -> REG_LAST_SAMPLE_VALUE_T;
            case HUMIDITY -> REG_LAST_SAMPLE_VALUE_H;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };
        return fixedpointToFloat(sensorType, bufferGetValue(offset));
    }

    /**
     * Gets last measurement by given sensor.
     *
     * @param sensorType Type of Sensor according to enum
     * @param preamble   Whether header shall be added
     * @param unit       Whether unit shall be added
     * @return Converted, human-readable value
     * @throws IllegalArgumentException If sensor does not exist.
     */
    public String getLastSample(BIN_SENSOR_TYPES sensorType, boolean preamble, boolean unit) {
        String output = "";
        if (preamble) {
            output += "LAST SAMPLE ";
            output += switch (sensorType) {
                case TEMPERATURE -> "(TEMPERATURE): ";
                case HUMIDITY -> "(HUMIDITY): ";
                case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
            };
        }
        output += Float.toString(getLastSample(sensorType));
        if (unit) {
            output += switch (sensorType) {
                case TEMPERATURE -> " °C";
                case HUMIDITY -> " %";
            };
        }
        return output;
    }

    /**
     * Gets total number of stored samples by given sensor.
     *
     * @param sensorType Type of Sensor according to enum
     * @return Converted, human-readable value
     * @throws IllegalArgumentException If sensor does not exist.
     */
    public short getSamplesNum(BIN_SENSOR_TYPES sensorType) {
        short offset = switch (sensorType) {
            case TEMPERATURE -> REG_SAMPLES_NUM_T;
            case HUMIDITY -> REG_SAMPLES_NUM_H;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };
        return bufferGetValue(offset);
    }

    /**
     * Gets total number of stored samples by given sensor.
     *
     * @param sensorType Type of Sensor according to enum
     * @param preamble   Whether header shall be added
     * @return Converted, human-readable value
     * @throws IllegalArgumentException If sensor does not exist.
     */
    public String getSamplesNum(BIN_SENSOR_TYPES sensorType, boolean preamble) {
        String output = "";
        if (preamble) {
            output += "SAMPLES COUNT ";
            output += switch (sensorType) {
                case TEMPERATURE -> "(TEMPERATURE): ";
                case HUMIDITY -> "(HUMIDITY): ";
                case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
            };
        }
        return output + Short.toString(getSamplesNum(sensorType));
    }

    /**
     * Gets Shipping Date (when logging was enabled) from tag.
     *
     * @return Formatted DateTime of shipping
     */
    public Date getShippingDate() {
        return shortsToDate(new short[]{
                bufferGetValue(REG_SHIPPING_DATE_H),
                bufferGetValue(REG_SHIPPING_DATE_L)
        });
    }

    /**
     * Gets Shipping Date (when logging was enabled) from tag.
     *
     * @param preamble Whether header shall be added
     * @return Formatted DateTime of shipping
     */
    public String getShippingDate(boolean preamble) {
        String output = "";
        if (preamble)
            output += "Shipping Date: ";
        //output += getShippingDate().toString();
        output += formatTimestamp(LocalDateTime.ofInstant(getShippingDate().toInstant(), ZoneOffset.ofHours(1))); // TODO: OK?
        return output;
    }

    /**
     * Gets Stop Date (when logging was disabled) from tag.
     *
     * @return Formatted DateTime of logging stop
     */
    public Date getStopDate() {
        return shortsToDate(new short[]{
                bufferGetValue(REG_STOP_DATE_H),
                bufferGetValue(REG_STOP_DATE_L)
        });
    }

    /**
     * Gets Stop Date (when logging was disabled) from tag.
     *
     * @param preamble Whether header shall be added
     * @return Formatted DateTime of logging stop
     */
    public String getStopDate(boolean preamble) {
        String output = "";
        if (preamble)
            output += "Stopped (logging) Date: ";
        //output += getStopDate().toString();
        output += formatTimestamp(LocalDateTime.ofInstant(getStopDate().toInstant(), ZoneOffset.ofHours(1))); // TODO: OK?
        return output;
    }

    /**
     * Gets User Area / specific information from tag.
     *
     * @return Raw data
     */
    public short[] getUserArea() {
        short[] dataToRead = new short[REG_USER_AREA_END - REG_USER_AREA_START + 1];
        for (int i = 0; i < dataToRead.length; i++)
            dataToRead[i] = bufferGetValue((short) (REG_USER_AREA_START + i));
        return dataToRead;
    }

    /**
     * Gets User Area / specific information from tag.
     *
     * @param preamble      Whether header shall be added
     * @param printAsString Whether to print Bytes as String
     * @param printAsTable  Whether to print Bytes as raw data with extra information.
     * @return Formatted table
     */
    public String getUserArea(boolean preamble, boolean printAsString, boolean printAsTable) {
        String output = "";
        if (preamble)
            output += "User Area: ";
        if (printAsString)
            output += new String(shortsToBytes(getUserArea()), StandardCharsets.UTF_8).trim();
        if (preamble)
            output += "\n" + spacer;
        if (printAsTable)
            output += tableifyReadData(getUserArea(), REG_USER_AREA_START, true) + spacer;
        return output;
    }

    /**
     * Sets User Area / specific information from tag.
     *
     * @param input Personal information to write to tag. Allows empty string.
     * @param info Print warnings
     */
    public void setUserArea(String input, boolean info) {
        short userAreaSize = (REG_USER_AREA_END - REG_USER_AREA_START + 1) * 2; // in bytes

        // Convert the input string to bytes (UTF-8 encoding for general compatibility)
        byte[] dataToPrep = input.getBytes(StandardCharsets.UTF_8);
        byte[] paddedData = new byte[userAreaSize];

        // Check if the data fits within the user area size
        if (dataToPrep.length > userAreaSize && info)
            System.out.println("Warning: Input exceeds user area size (" + dataToPrep.length + "/" + userAreaSize + " bytes). Will cut off the rest!");

        // If input is smaller than user area, pad with zeros to fill the area
        for (int i = 0; i < paddedData.length; i++) {
            if (i < dataToPrep.length)
                paddedData[i] = dataToPrep[i];
            else
                paddedData[i] = 0;
        }

        // PackBytes2Shorts and Write the data to the user area in chunks of words (2 bytes each)
        short[] data2write = bytesToShorts(paddedData);
        for (int i = 0; i < data2write.length; i++) {
            if (info)
                progressPercentage(i, data2write.length - 1);
            bufferUpdateValue((short) (REG_USER_AREA_START + i), data2write[i]);
        }

        if (info)
            System.out.println("Data successfully written to user area.");
    }

    /**
     * Gets raw Log Area from tag by given sensor.
     *
     * @param sensorType Type of Sensor according to enum
     * @return Raw data
     * @throws IllegalArgumentException if sensor doesnt exist.
     */
    public short[] getLogArea(BIN_SENSOR_TYPES sensorType) {
        short offset = switch (sensorType) {
            case TEMPERATURE -> REG_LOG_AREA_T_START;
            case HUMIDITY -> REG_LOG_AREA_H_START;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };
        short len = switch (sensorType) {
            case TEMPERATURE -> REG_LOG_AREA_T_END - REG_LOG_AREA_T_START + 1;
            case HUMIDITY -> REG_LOG_AREA_H_END - REG_LOG_AREA_H_START + 1;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };
        short[] dataToRead = new short[len];
        for (int i = 0; i < dataToRead.length; i++)
            dataToRead[i] = bufferGetValue((short) (offset + i));
        return dataToRead;
    }

    /**
     * Gets raw Log Area from tag by given sensor.
     *
     * @param sensorType Type of Sensor according to enum
     * @param preamble   Whether header shall be added
     * @return Raw data
     * @throws IllegalArgumentException If sensor doesnt exist.
     */
    public String getLogArea(BIN_SENSOR_TYPES sensorType, boolean preamble) {
        String output = "";
        if (preamble) {
            output += "Log Area\n" + spacer;
            output += switch (sensorType) {
                case TEMPERATURE -> "(TEMPERATURE):\n";
                case HUMIDITY -> "(HUMIDITY):\n";
                case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
            };
        }
        short offset = switch (sensorType) {
            case TEMPERATURE -> REG_LOG_AREA_T_START;
            case HUMIDITY -> REG_LOG_AREA_H_START;
            case null, default -> throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
        };
        return output + tableifyReadData(getLogArea(sensorType), offset, true) + spacer;
    }

    /////////////////////// EXPORT FUNCTIONS //////////////////////

    /**
     * Function to parse log data by sensor directly into a list of measurement points.
     *
     * @param sensor Type of Sensor according to enum
     * @return List of measurement points
     * @throws IllegalArgumentException If sensor doesnt exist.
     * @throws RuntimeException         If errors occur at data translation.
     * @return List of measurement points
     */
    public List<measurmentPoint> parseLogData(RT0013.BIN_SENSOR_TYPES sensor) {
        if (sensor == null || (sensor != BIN_SENSOR_TYPES.TEMPERATURE && sensor != BIN_SENSOR_TYPES.HUMIDITY))
            throw new IllegalArgumentException("Unknown sensorType: " + sensor);
        if (getSamplesNum(sensor) <= 0) {
            throw new RuntimeException("Samples number must be greater than zero!");
        }

        // Get context
        short mode = 0;
        for (short i = 0; i <= 5; i++)
            if (getBinEnX(i, BIN_EN_TYPES.TIME_STORE, sensor))
                mode = (short) setBit(mode, 0, true);

        for (short i = 0; i <= 5; i++)
            if (getBinEnX(i, BIN_EN_TYPES.SAMPLE_STORE, sensor))
                mode = (short) setBit(mode, 1, true);

        // Interpret data
        short[] logArea = getLogArea(sensor);

        short emptyMarker = (short) 0xFFFF;
        short valueBufferSh = emptyMarker;
        short[] dateBuffer = new short[2];

        List<measurmentPoint> dataTable = new ArrayList<>();
        dataTable.add(new measurmentPoint(null, emptyMarker));

        for (int ctr = 0; (ctr < logArea.length && dataTable.size() < getSamplesNum(sensor)); ctr++) {
            if (logArea[ctr] == emptyMarker) // check if early end of log area
                break;

            // act according to cases
            switch (mode) {
                case 0 -> throw new RuntimeException("Nothing to export as config invalid!");
                case 1 -> {
                    // Get from buffer
                    dateBuffer[1] = (ctr % 2 == 0) ? logArea[ctr] : dateBuffer[1];
                    dateBuffer[0] = (ctr % 2 == 1) ? logArea[ctr] : dateBuffer[0];

                    // Conditionally put to List and add up
                    if (dateBuffer[0] != emptyMarker && dateBuffer[1] != emptyMarker)
                        dataTable.getLast().setDate(shortsToDate(dateBuffer));
                    if (dataTable.getLast().getDate() != null)
                        dataTable.add(new measurmentPoint(null, emptyMarker));
                }
                case 2 -> {
                    // Get from buffer
                    valueBufferSh = logArea[ctr];

                    // Conditionally put to List and add up
                    if (valueBufferSh != emptyMarker && isValidValue(sensor, fixedpointToFloat(sensor, valueBufferSh)))
                        dataTable.getLast().setValue(fixedpointToFloat(sensor, valueBufferSh));
                    if (dataTable.getLast().getValue() != emptyMarker)
                        dataTable.add(new measurmentPoint(null, emptyMarker));
                }
                case 3 -> {
                    // Get from buffer
                    valueBufferSh = (ctr % 3 == 0) ? logArea[ctr] : valueBufferSh;
                    dateBuffer[1] = (ctr % 3 == 1) ? logArea[ctr] : dateBuffer[1];
                    dateBuffer[0] = (ctr % 3 == 2) ? logArea[ctr] : dateBuffer[0];

                    // Conditionally put to List and add up
                    if (dateBuffer[0] != emptyMarker && dateBuffer[1] != emptyMarker)
                        dataTable.getLast().setDate(shortsToDate(dateBuffer));
                    if (valueBufferSh != emptyMarker && isValidValue(sensor, fixedpointToFloat(sensor, valueBufferSh)))
                        dataTable.getLast().setValue(fixedpointToFloat(sensor, valueBufferSh));
                    if (dataTable.getLast().getDate() != null && dataTable.getLast().getValue() != emptyMarker)
                        dataTable.add(new measurmentPoint(null, emptyMarker));
                }
                default -> throw new RuntimeException("Unknown mode: " + mode);
            }
        }

        // Sort table by timestamp if present
        if (getBit(mode, 0))
            dataTable.sort(
                    Comparator.comparing(measurmentPoint::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
            );
        dataTable.removeLast();

        return dataTable;
    }
}

