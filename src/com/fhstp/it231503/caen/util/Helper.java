package com.fhstp.it231503.caen.util;

import com.fhstp.it231503.caen.rfid.RT0013;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
/**
 * Helper static class for sharing functions in implementations.
 * @author Emil Sedlacek / it231503
 * */
public class Helper {

    // Often used strings
    public static final String spacer = "--------------------------------------\n";
    public static final String continueQuestion = "Do you wish to continue - Y(es) or N(o)/<any>?\nAnswer (N): ";

    ////////////////////////////////////////// DATATYPE CONVERSION //////////////////////////////////////////

    /**
     * Function to get bit from a certain position.
     *
     * @param value Value in range of 63/31 Bits (depending on system) as signed value.
     * @param bit   Position of said bit
     * @return Requested bit state
     */
    public static boolean getBit(int value, int bit) {
        return ((value >> bit) & 1) == 1;
    }

    /**
     * Function to set a bit at a certain position.
     *
     * @param value Value
     * @param bit   Position of said bit
     * @param state New state
     * @return Modified value
     */
    public static int setBit(int value, int bit, boolean state) {
        if (state)
            value |= (1 << bit);
        else
            value &= ~(1 << bit);
        return value;
    }

    /**
     * Function to convert array of Bytes to ONE Short.
     *
     * @implNote Assuming big-endian order!
     */
    public static short bytesToShort(byte[] values) {
        if (values.length > 2) {
            throw new IllegalArgumentException("Error: Too many bytes!");
        }
        short value = 0;
        for (short b : values)
            value = (short) ((value << 8) + (b & 0xFF));
        return value;
    }

    /**
     * Function to convert array of bytes to array of shorts.
     *
     * @implNote Be aware this functions must have an even lengthened array. Assuming big-endian order!
     */
    public static short[] bytesToShorts(byte[] dataToRead) {
        if (dataToRead.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of bytes to read.");
        }

        short[] shortArray = new short[dataToRead.length / 2];
        for (short i = 0; i < dataToRead.length; i += 2)
            shortArray[i / 2] = (short) ((dataToRead[i] << 8) | (dataToRead[i + 1] & 0xFF));
        return shortArray;
    }

    /**
     * Function to convert array of bytes to int.
     *
     * @implNote Assuming big-endian order!
     */
    public static int bytesToInt(byte[] data) {
        if (data.length > 4)
            throw new IllegalArgumentException("Error: Too many bytes!");

        int value = 0;
        for (byte b : data) {
            value = (value << 8) + (b & 0xFF);
        }
        return value;
    }

    /**
     * Function to convert array of bytes to Hex String.
     *
     * @implNote Assuming big-endian order!
     */
    public static String bytesToHexstring(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Function to convert array of shorts to int.
     *
     * @implNote Assuming big-endian order!
     */
    public static int shortsToInt(short[] data) {
        if (data.length > 2) {
            throw new IllegalArgumentException("Error: Too many bytes!");
        }
        int value = 0;
        for (short b : data) {
            value = (value << 16) + (b & 0xFFFF);
        }
        return value;
    }

    /**
     * Function to convert array of shorts to array of bytes.
     *
     * @implNote Big-endian order per short!
     */
    public static byte[] shortsToBytes(short[] data) {
        byte[] temp = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            temp[i * 2] = shortToBytes(data[i])[0];
            temp[i * 2 + 1] = shortToBytes(data[i])[1];
        }
        return temp;
    }

    /**
     * Function to convert short to array of bytes.
     *
     * @implNote Big-endian order per short!
     */
    public static byte[] shortToBytes(short data) {
        byte[] ret = new byte[2];
        ret[0] = (byte) ((data >> 8) & 0xff);
        ret[1] = (byte) (data & 0xff);
        return ret;
    }

    /**
     * Function to convert shorts to Date.
     *
     * @param data measured in milliseconds.
     * @implNote Big-endian order!
     */
    public static Date shortsToDate(short[] data) {
        if (data.length > 2)
            throw new IllegalArgumentException("Error: Too many shorts!");
        return new Date(new Timestamp(shortsToInt(data) * 1000L).getTime()); // seconds to miliseconds
    }

    /**
     * Function to convert Date to shorts.
     *
     * @return Value measured in milliseconds.
     * @implNote Big-endian order!
     */
    public static short[] dateToShorts(Date date) {
        int timeInt = (int) (date.getTime() / 1000); // to seconds
        short[] data = new short[2];
        data[0] = (short) (timeInt >> 16);      // High word
        data[1] = (short) (timeInt & 0xFFFF);   // Low word
        return data;
    }

    /**
     * Function to convert int to shorts.
     *
     * @implNote Big-endian order!
     */
    public static short[] intToShorts(int value) {
        short[] data = new short[2];
        data[0] = (short) ((value >> 16) & 0xFFFF);  // High word
        data[1] = (short) (value & 0xFFFF);          // Low word
        return data;
    }

    /**
     * Function to convert Short to Hex String.
     *
     * @implNote Big-endian order!
     */
    public static String shortToHexString(short value) {
        return String.format("%02X", value);
    }


    ////////////////////////////////////////// DATA CONVERSION //////////////////////////////////////////

    /**
     * Function to convert float temperature/humidity value according to RT0013-encoding to short.
     *
     * @param sensor Type of Sensor
     * @param value  in °C or %
     * @return Register-friendly short
     * @throws IllegalArgumentException When wrong sensor is given.
     */
    public static short floatToFixedpoint(RT0013.BIN_SENSOR_TYPES sensor, float value) {
        if (sensor == RT0013.BIN_SENSOR_TYPES.TEMPERATURE) {
            if (value >= 0 && value <= 70) {
                return (short) (value * 32);
            } else if (value > 70) {
                return (short) (70. * 32);
            } else if (value < 0 && value >= -30) {
                return (short) (value * 32 + 8192);
            } else if (value < -30) {
                return (short) (-30 * 32 + 8192);
            } else
                return 0;
        } else if (sensor == RT0013.BIN_SENSOR_TYPES.HUMIDITY) {
            if (value > 100) {
                return (short) (100 * 32);
            } else if (value >= 0 && value <= 100) {
                return (short) (value * 32);
            } else
                return (short) 0;
        } else
            throw new IllegalArgumentException("Wrong sensor given");
    }

    /**
     * Function to convert register short according to RT0013-encoding to float temperature/humidity value.
     *
     * @param sensor     Type of Sensor
     * @param fixedPoint Register temperature/humidity value
     * @return Value in °C or %
     * @throws IllegalArgumentException When wrong sensor is given.
     */
    public static float fixedpointToFloat(RT0013.BIN_SENSOR_TYPES sensor, short fixedPoint) {
        if (sensor == RT0013.BIN_SENSOR_TYPES.TEMPERATURE) {
            if (fixedPoint >= 0 && fixedPoint <= 70 * 32) {
                return fixedPoint / 32.0f;
            } else if (fixedPoint > 70 * 32 && fixedPoint < 7232) {
                return 70.0f;
            } else if (fixedPoint >= 7232 && fixedPoint < 8192) {
                return (fixedPoint - 8192) / 32.0f;
            } else {
                return 0;
            }
        } else if (sensor == RT0013.BIN_SENSOR_TYPES.HUMIDITY) {
            if (fixedPoint > 100 * 32.0) {
                return 100f;
            } else if (fixedPoint >= 0 && fixedPoint <= 100 * 32) {
                return fixedPoint / 32.0f;
            } else
                return 0f;
        } else
            throw new IllegalArgumentException("Wrong sensor given");
    }


    /**
     * Function to check valid float temperature/humidity value for RT0013.
     *
     * @param sensor Type of Sensor
     * @param value  in °C or %
     * @return True if valid value.
     * @throws RuntimeException When wrong sensor is given.
     */
    public static boolean isValidValue(RT0013.BIN_SENSOR_TYPES sensor, Float value) {
        if (sensor == RT0013.BIN_SENSOR_TYPES.TEMPERATURE) {
            return value >= -30 && value <= 70;  // Temperature range
        } else if (sensor == RT0013.BIN_SENSOR_TYPES.HUMIDITY) {
            return value >= 0 && value <= 100;  // Humidity range
        } else
            throw new IllegalArgumentException("Wrong sensor given");
    }

    /////////////////////// Generic Display

    /**
     * Function to show a small progressbar based on input.
     *
     * @param remain Remaining amount until total.
     * @param total  Total value to calculate percent from.
     * @implNote Does edit last line. Better don't output something else!
     */
    public static void progressPercentage(int remain, int total) {
        if (remain <= total) {
            int maxBareSize = 10; // 10unit for 100%
            int remainProcent = ((100 * remain) / total) / maxBareSize;
            char defaultChar = '-';
            String icon = "*";
            String bare = new String(new char[maxBareSize]).replace('\0', defaultChar) + "]";
            StringBuilder bareDone = new StringBuilder();
            bareDone.append("[");
            bareDone.append(icon.repeat(Math.max(0, remainProcent)));
            String bareRemain = bare.substring(remainProcent);
            System.out.print("\r" + bareDone + bareRemain + " " + remainProcent * 10 + "%");
            if (remain == total) {
                System.out.print("\n");
            }
        }
    }

    /**
     * Function to pretty-format data from short-array and print corresponding address next to it.
     *
     * @param dataToRead        Raw data to display.
     * @param offsetWordaddress Address of first element in array
     * @param preamble          Whether Table header and tabulators shall be added
     * @return String with formated table.
     * @implNote Assumption: No address jumps.
     */
    public static String tableifyReadData(short[] dataToRead, short offsetWordaddress, boolean preamble) {
        StringBuilder output = new StringBuilder();

        if (dataToRead != null && dataToRead.length >= 1) {
            if (preamble) output.append("Address : Hex\t | BIN\t\t\t\t\t\t\t\t| CHAR\n");
            for (short x = 0; x < dataToRead.length; x++) {
                if (preamble)
                    output.append("\t");
                output.append(String.format("0x%02X: ", (offsetWordaddress + x)));
                output.append(String.format("0x%04X | ", dataToRead[x]));
                output.append(String.format("%32s | ", Integer.toBinaryString(dataToRead[x])));

                for (int j = 0; j <= 1; j++) {
                    try {
                        output.append(String.format("%c ", shortToBytes(dataToRead[x])[j]).replace("\n", "\0"));
                    } catch (IllegalFormatCodePointException e) {
                        output.append("? ");
                    }
                }
                output.append("\n");
            }
        } else {
            output = new StringBuilder("No data to be shown.");
        }
        return output.toString();
    }


    ////////////////////////////////////////// INPUT CONVERSION //////////////////////////////////////////

    /**
     * Function to prompt given string and return user input by given scanner.
     *
     * @param scanner High level object to get user input.
     * @param display Text to print before prompting input.
     * @return User input
     */
    public static String promptAndGetInput(Scanner scanner, String display) {
        System.out.print(display);
        return scanner.nextLine();
    }

    /**
     * Function to prompt given string and return processed user input by given scanner.
     *
     * @param input        User input
     * @param defaultValue Value to fill in if value is not parseable.
     * @return Processed user input
     */
    public static boolean parseBooleanInput(String input, boolean defaultValue) {
        if (input == null || input.isEmpty())
            return defaultValue;

        switch (input.trim().toLowerCase()) {
            case "true", "t", "yes", "y", "1" -> {
                return true;
            }
            case "false", "f", "no", "n", "0" -> {
                return false;
            }
            default -> {
                return defaultValue;
            }
        }
    }

    /**
     * Function to prompt given string and return processed user input by given scanner.
     *
     * @param input        User input
     * @param defaultValue Value to fill in if value is not parseable.
     * @return Processed user input
     */
    public static short parseHexInput(String input, short defaultValue) {
        if (input == null || input.isEmpty())
            return defaultValue;
        try {
            return (short) Integer.parseInt(input, 16);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Function to prompt given string and return processed user input by given scanner.
     *
     * @param input        User input
     * @param defaultValue Value to fill in if value is not parseable.
     * @return Processed user input
     */
    public static short parseShortInput(String input, short defaultValue) {
        if (input == null || input.isEmpty())
            return defaultValue;

        try {
            return Short.parseShort(input);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Function to prompt given string and return processed user input by given scanner.
     *
     * @param input        User input
     * @param defaultValue Value to fill in if value is not parseable.
     * @return Processed user input
     */
    public static int parseIntInput(String input, int defaultValue) {
        if (input == null || input.isEmpty())
            return defaultValue;

        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Function to prompt given string and return processed user input by given scanner.
     *
     * @param input        User input
     * @param defaultValue Value to fill in if value is not parseable.
     * @return Processed user input
     */
    public static float parseFloatInput(String input, float defaultValue) {
        if (input == null || input.isEmpty())
            return defaultValue;

        try {
            return Float.parseFloat(input);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Function to prompt and process user input to a date by given scanner.
     * Asks for year, month, day, hour and minute.
     *
     * @param scanner User input
     * @return Date inputed by user
     */
    public static Date promptAndParseDateTime(Scanner scanner) {
        short year = promptForYear((short) 1970, scanner); // Default year
        short month = promptForMonth((short) 1, scanner);  // Default to January
        short day = promptForDay(year, month, (short) 1, scanner); // Default to 1st day
        short hour = promptForHour((short) 1, scanner);    // Default to midnight
        short minute = promptForMinute((short) 0, scanner); // Default to 0 minutes

        // Use Calendar to construct a Date object
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, hour, minute, 0); // month-1 because Calendar months are 0-indexed
        calendar.set(Calendar.MILLISECOND, 0); // Set milliseconds to zero

        return calendar.getTime();
    }

    /**
     * Function to prompt for year and return processed user input by given scanner.
     *
     * @param defaultValue Standard year
     * @param scanner      User input object
     * @return Chosen year
     */
    public static short promptForYear(short defaultValue, Scanner scanner) {
        short year = defaultValue;
        for (int i = 1; i <= 3; i++) {
            year = parseShortInput(promptAndGetInput(scanner, "Enter year (e.g., 2023) [default: " + defaultValue + "]: "), defaultValue);
            if (year < 1970 || year > 2035) {
                System.err.println("Invalid year. Please enter a value between 1970 and 2035.");
            } else
                break;
        }
        return year;
    }

    /**
     * Function to prompt for month and return processed user input by given scanner.
     *
     * @param defaultValue Standard month
     * @param scanner      User input object
     * @return Chosen month
     */
    public static short promptForMonth(short defaultValue, Scanner scanner) {
        short month = defaultValue;
        for (int i = 1; i <= 3; i++) {
            month = parseShortInput(promptAndGetInput(scanner, "Enter month (1-12) [default: " + defaultValue + "]: "), defaultValue);
            if (month < 1 || month > 12) {
                System.err.println("Invalid month. Please enter a value between 1 and 12.");
            } else
                break;
        }
        return month;
    }

    /**
     * Function to prompt for day and return processed user input by given scanner.
     *
     * @param defaultValue Standard day
     * @param scanner      User input object
     * @return Chosen day
     */
    public static short promptForDay(short year, short month, short defaultValue, Scanner scanner) {
        short day = defaultValue;
        int maxDay = switch (month) { // get
            case 2 -> ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) ? 29 : 28; // is leap year?
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
        for (int i = 1; i <= 3; i++) {
            day = parseShortInput(promptAndGetInput(scanner, "Enter day (1-31 based on month) [default: " + defaultValue + "]: "), defaultValue);
            if (day < 1 || day > maxDay) {
                System.err.println("Invalid day. Please enter a value between 1 and " + maxDay);
            } else
                break;
        }
        return day;
    }

    /**
     * Function to prompt for hour and return processed user input by given scanner.
     *
     * @param defaultValue Standard hour
     * @param scanner      User input object
     * @return Chosen hour
     */
    public static short promptForHour(short defaultValue, Scanner scanner) {
        short hour = defaultValue;
        for (int i = 1; i <= 3; i++) {
            hour = parseShortInput(promptAndGetInput(scanner, "Enter hour (0-23) [default: " + defaultValue + "]: "), defaultValue);
            if (hour < 0 || hour > 23) {
                System.err.println("Invalid hour. Please enter a value between 0 and 23.");
            } else
                break;
        }
        return hour;
    }

    /**
     * Function to prompt for minute and return processed user input by given scanner.
     *
     * @param defaultValue Standard minute
     * @param scanner      User input object
     * @return Chosen minute
     */
    public static short promptForMinute(short defaultValue, Scanner scanner) {
        short minute = defaultValue;
        for (int i = 1; i <= 3; i++) {
            minute = parseShortInput(promptAndGetInput(scanner, "Enter minute (0-59) [default: " + defaultValue + "]: "), defaultValue);
            if (minute < 0 || minute > 59) {
                System.err.println("Invalid minute!");
            } else
                break;
        }
        return minute;
    }

    /**
     * Function to pretty-format date.
     *
     * @param dateTime Sorted and checked array of binSettings.
     * @return String with formated date.
     */
    public static String formatTimestamp(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


    /////////////////////// binSetting - Object //////////////////////

    /**
     * Function to pretty-format data from short-array and print corresponding address next to it.
     *
     * @param settings Sorted and checked array of binSettings.
     * @param sensor   Type of Sensor
     * @return String with formated table.
     */
    public static String printTable(List<binSetting> settings, RT0013.BIN_SENSOR_TYPES sensor) {
        if (settings == null || settings.isEmpty())
            return "Nothing to print!";

        StringBuilder table = new StringBuilder();
        int ctr = 0;
        float start;
        if (sensor == RT0013.BIN_SENSOR_TYPES.TEMPERATURE) {
            table.append("Bin #\t| Low Limit °C\t| High Limit °C\t| Threshold\t| Store Samples\t| Store Times\t| Sampling Interval (sec)\n");
            start = -70f;
        } else if (sensor == RT0013.BIN_SENSOR_TYPES.HUMIDITY) {
            table.append("Bin #\t| Low Limit %\t| High Limit %\t| Threshold\t| Store Samples\t| Store Times\t| Sampling Interval (sec)\n");
            start = 0f;
        } else
            throw new IllegalArgumentException("Wrong sensor given");

        for (binSetting s : settings) {
            table.append(String.format("%5d\t| %11f\t| %12f\t| %7d\t| %13b\t| %13b\t| %22d\n",
                    ctr, // BIN
                    (ctr == 0) ? start : settings.get(ctr - 1).getHighLimit(), // LOWLIMIT
                    s.getHighLimit(),
                    s.getThreshold(),
                    s.isStoreSamples(),
                    s.isStoreTimes(),
                    s.getSamplingInterval())
            );
            ctr++;
        }
        return table.toString();
    }

    /**
     * Function to retrieve binSettings from guided user.
     *
     * @param scanner User input object
     * @param sensor  Type of Sensor
     * @return Sorted and checked array of binSettings.
     */
    public static List<binSetting> inputTable(RT0013.BIN_SENSOR_TYPES sensor, Scanner scanner) {
        System.out.println(spacer + "Now, please enter your wished values accordingly. You will be asked for each bin.");

        List<binSetting> settings = new ArrayList<>();
        for (short i = 0; i <= 5; i++) {
            System.out.println("Preparing bin #" + i + " for " + ((sensor == RT0013.BIN_SENSOR_TYPES.TEMPERATURE) ? "TEMPERATURE" : "HUMIDITY") + "...");
            if (!parseBooleanInput(promptAndGetInput(scanner, "You could add this bin (Y(es)) or finish table (N(o)/<any>).\n" + continueQuestion), false))
                return settings;

            //Input
            if (sensor == RT0013.BIN_SENSOR_TYPES.TEMPERATURE)
                settings.add(new binSetting(
                        parseFloatInput(promptAndGetInput(scanner, "What temperature (°C) shall this bin go to?\nHigh Limit (70): "), 70f),
                        parseShortInput(promptAndGetInput(scanner, "How often shall this bin log data until triggering an alarm?\nCounter threshold (unlimited=never): "), (short) 0xFFFF),
                        parseBooleanInput(promptAndGetInput(scanner, "Should the tag log all datapoints in this bin? Y(es)/<any> or N(o)\nLog data (Y): "), true),
                        parseBooleanInput(promptAndGetInput(scanner, "Should the tag log all times in this bin? Y(es)/<any> or N(o)\nLog time (Y): "), true),
                        parseShortInput(promptAndGetInput(scanner, "What interval (sec) shall this bin collect data?\nInterval (30): "), (short) 0x1E)
                ));

            else if (sensor == RT0013.BIN_SENSOR_TYPES.HUMIDITY)
                settings.add(new binSetting(
                        parseFloatInput(promptAndGetInput(scanner, "What humidity (%) shall this bin go to?\nHigh Limit (100): "), 100f),
                        parseShortInput(promptAndGetInput(scanner, "How often shall this bin log data until triggering an alarm?\nCounter threshold (unlimited=never): "), (short) 0xFFFF),
                        parseBooleanInput(promptAndGetInput(scanner, "Should the tag log all datapoints in this bin? Y(es)/<any> or N(o)\nLog data (Y): "), true),
                        parseBooleanInput(promptAndGetInput(scanner, "Should the tag log all times in this bin? Y(es)/<any> or N(o)\nLog time (Y): "), true),
                        parseShortInput(promptAndGetInput(scanner, "What interval (sec) shall this bin log data?\nInterval (30): "), (short) 0x1E)
                ));
            else
                throw new IllegalArgumentException("Wrong sensor given");

            // Check for correct input
            if (i > 0 && settings.get(i).getHighLimit() < settings.get(i - 1).getHighLimit()) {
                System.out.println("Warning: The lowLimit always connects to the last highLimit. Will correct it accordingly...");
                settings.get(i).setHighLimit(settings.get(i - 1).getHighLimit() + 1f);
            }
        }

        return settings;
    }

    /**
     * Function to retrieve binSettings from guided user. Data is eventually modified.
     *
     * @param currentSettings Current array of binSettings.
     * @param scanner         User input object
     * @param sensor          Type of Sensor
     * @return Sorted and checked array of binSettings.
     * @implNote Is separated from inputTable() due to the size.
     */
    public static List<binSetting> promptBinConfig(RT0013.BIN_SENSOR_TYPES sensor, List<binSetting> currentSettings, Scanner scanner) {
        if (sensor != RT0013.BIN_SENSOR_TYPES.TEMPERATURE && sensor != RT0013.BIN_SENSOR_TYPES.HUMIDITY)
            throw new IllegalArgumentException("Wrong sensor given");

        // Print current settings
        System.out.println("This is the current configuration. Please proofread!\n" + printTable(currentSettings, sensor));

        // Ask for new settings
        if (!parseBooleanInput(promptAndGetInput(scanner, "You could use this configuration.\n" + continueQuestion), false)) {
            List<binSetting> settings = new ArrayList<>();

            // Sampling inputs with parsing functions
            System.out.print("This is an example table how the configuration for ");
            if (sensor == RT0013.BIN_SENSOR_TYPES.TEMPERATURE) {
                System.out.println("temperature.");
                settings.add(new binSetting(-30f, (short) 3, true, true, (short) 30));
                settings.add(new binSetting(-15f, (short) 10, true, true, (short) 45));
                //settings.add(new Setting(0f, (short) 0xFFFF, false, false, (short) 120));
                settings.add(new binSetting(0f, (short) 60, true, true, (short) 120));
                //settings.add(new Setting(30f, (short) 0xFFFF, false, false, (short) 120));
                settings.add(new binSetting(30f, (short) 60, true, true, (short) 120));
                settings.add(new binSetting(50f, (short) 10, true, true, (short) 60));
                settings.add(new binSetting(70f, (short) 3, true, true, (short) 30));
            } else if (sensor == RT0013.BIN_SENSOR_TYPES.HUMIDITY) {
                System.out.println("humidty.");
                settings.add(new binSetting(20f, (short) 5, true, true, (short) 30));
                settings.add(new binSetting(40f, (short) 10, true, true, (short) 60));
                //settings.add(new Setting(60f, (short) 0xFFFF, false, false, (short) 120));
                settings.add(new binSetting(60f, (short) 60, true, true, (short) 120));
                settings.add(new binSetting(80f, (short) 10, true, true, (short) 60));
                settings.add(new binSetting(100f, (short) 5, true, true, (short) 30));
            } else
                throw new IllegalArgumentException("Wrong sensor given");
            System.out.println(spacer + printTable(settings, sensor)); // Print here hardcoded settings

            if (parseBooleanInput(promptAndGetInput(scanner, spacer + "You could directly use this as a tag test.\n" + continueQuestion), false))
                return settings;

            if (parseBooleanInput(promptAndGetInput(scanner, "You could create a new table.\n" + continueQuestion), false)) {
                settings = inputTable(sensor, scanner);
                if (settings != null && !settings.isEmpty()) {
                    System.out.println(printTable(settings, sensor)); // Print inputed settings

                    if (parseBooleanInput(promptAndGetInput(scanner, "You could use this new table.\n" + continueQuestion), false))
                        return settings;
                }
            }
        }

        System.out.println("Using existing table then...");
        return currentSettings;
    }

    /**
     * Function to pretty-format data from array of measurmentPoints and print corresponding datapoints.
     *
     * @param dataTable Sorted and checked array of measurmentPoints.
     * @param sensor    Type of Sensor
     * @param limit     Limits the count of datapoints printed by descending date. (Latest)
     * @return String with formated table.
     */
    public static String displayDataTable(List<measurmentPoint> dataTable, RT0013.BIN_SENSOR_TYPES sensor, short limit) {
        StringBuilder output = new StringBuilder("Timestamp\t\t\t\t");
        if (sensor == RT0013.BIN_SENSOR_TYPES.TEMPERATURE) {
            output.append("Temperature (°C)");
        } else if (sensor == RT0013.BIN_SENSOR_TYPES.HUMIDITY) {
            output.append("Humidity (%)");
        } else
            throw new IllegalArgumentException("Wrong sensor given");
        output.append("\n");

        int rowCount = dataTable.size();
        for (short i = (short) Math.max(0, rowCount - limit); i < rowCount; i++) {
            Date timestamp = dataTable.get(i).getDate();
            Float value = dataTable.get(i).getValue();
            output.append((timestamp != null) ? timestamp.toInstant().toString() : "nan")
                    .append("\t")
                    .append((value != 0) ? value.toString() : "nan").append("\n");
        }
        return output.toString();
    }

    //WRITER

    /**
     * Function to write String to a TXT-file.
     *
     * @param content  String to write to disk.
     * @param fileName Name of TXT-file.
     */
    public static void exportStringToTxt(String fileName, String content) {
        if (content == null)
            return;

        try (FileWriter writer = new FileWriter(fileName + ".txt")) {
            writer.write(content);
            System.out.println("File added: " + fileName + ".txt");
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file.");
            e.printStackTrace();
        }
    }

    /**
     * Function to write array of measurmentPoints to a CSV file.
     *
     * @param dataTable Sorted and checked array of measurmentPoints.
     * @param sensor    Type of Sensor
     * @param fileName  Name of CSV-file.
     */
    public static void exportDataToCSV(String fileName, List<measurmentPoint> dataTable, RT0013.BIN_SENSOR_TYPES sensor) {
        if (dataTable == null)
            return;

        StringBuilder output = new StringBuilder();

        // Header
        output.append("Timestamp;");
        if (sensor == RT0013.BIN_SENSOR_TYPES.TEMPERATURE) {
            output.append("Temperature (°C)");
        } else if (sensor == RT0013.BIN_SENSOR_TYPES.HUMIDITY) {
            output.append("Humidity (%)");
        } else
            throw new IllegalArgumentException("Wrong sensor given");
        output.append("\n");

        // Data rows
        short emptyMarker = (short) 0xFFFF;
        for (int i = 0; i < dataTable.size(); i++) {
            Date timestamp = dataTable.get(i).getDate();
            Float value = dataTable.get(i).getValue();
            String timestampStr = (timestamp != null) ? DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss").format(LocalDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC)) : "nan";
            String valueStr = (value != emptyMarker) ? value.toString().replace(".", ",") : "nan";
            output.append(timestampStr)
                    .append(";")
                    .append(valueStr)
                    .append("\n");
        }

        fileName += ".csv";
        try (FileWriter csvWriter = new FileWriter(fileName)) {
            csvWriter.write(output.toString());
            System.out.println("File added: " + fileName);
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the CSV file.");
            e.printStackTrace();
        }
    }
}
