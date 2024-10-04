import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Prettifier {

    public static final String RESET = "\033[0m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String RED = "\033[0;31m";

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("-h")) {
            printUsage();
            return;
        }
        if (args.length != 3) {
            printUsage();
            return;
        }
        String inputFilePath = args[0];
        String outputFilePath = args[1];
        String airportLookupFilePath = args[2];

        if (!Files.exists(Paths.get(inputFilePath))) {
            System.out.println(RED + "Input not found" + RESET);
            return;
        }
        if (!Files.exists(Paths.get(airportLookupFilePath))) {
            System.out.println(RED + "Airport lookup not found" + RESET);
            return;
        }

        try {
            Map<String, String[]> airportData = loadAirportLookup(airportLookupFilePath);
            if (airportData == null) {
                System.out.println(RED + "Airport lookup malformed" + RESET);
                return;
            }

            List<String> inputLines = Files.readAllLines(Paths.get(inputFilePath));
            List<String> prettifiedLines = prettifyItinerary(inputLines, airportData);

            Files.write(Paths.get(outputFilePath), prettifiedLines);
        } catch (IllegalArgumentException e) {
            System.out.println(RED + "Airport lookup malformed" + RESET);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String[]> loadAirportLookup(String airportLookupFilePath) {
        Map<String, String[]> airportMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(airportLookupFilePath))) {
            String line = br.readLine();
            if (line == null)
                return null;

            String[] headers = line.split(",");
            int nameIndex = -1, cityIndex = -1, icaoIndex = -1, iataIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                switch (headers[i].trim()) {
                    case "name":
                        nameIndex = i;
                        break;
                    case "municipality":
                        cityIndex = i;
                        break;
                    case "icao_code":
                        icaoIndex = i;
                        break;
                    case "iata_code":
                        iataIndex = i;
                        break;
                }
            }

            if (nameIndex == -1 || cityIndex == -1 || icaoIndex == -1 || iataIndex == -1) {
                throw new IllegalArgumentException("CSV file does not contain all required columns");
            }

            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length > Math.max(Math.max(nameIndex, cityIndex), Math.max(icaoIndex, iataIndex))) {
                    String name = fields[nameIndex];
                    String city = fields[cityIndex];
                    String icao = fields[icaoIndex];
                    String iata = fields[iataIndex];

                    if (iata != null && !iata.isEmpty()) {
                        airportMap.put(iata, new String[] { name, city });
                    }
                    if (icao != null && !icao.isEmpty()) {
                        airportMap.put(icao, new String[] { name, city });
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return airportMap;
    }

    private static List<String> prettifyItinerary(List<String> inputLines, Map<String, String[]> airportData) {
        List<String> outputLines = new ArrayList<>();
        Prettifier converter = new Prettifier();

        for (String line : inputLines) {
            line = convertWhiteSpace(line);
            line = converter.convertCodes(line, airportData); // Используем новый метод
            line = convertDatesAndTimes(line);

            if (line.trim().isEmpty()) {
                if (outputLines.size() > 0 && !outputLines.get(outputLines.size() - 1).isEmpty()) {
                    outputLines.add("");
                }
            } else {
                outputLines.add(line);
            }
        }

        System.out.println(" ____  ____  ____  ____  ____  ____  ____  ____  ____  ____ ");
        System.out.println("|    ||    ||    ||    ||    ||    ||    ||    ||    ||    |");
        System.out.println("| P  || R  || E  || T  || T  || I  || F  || I  || E  || R  |");
        System.out.println("|____||____||____||____||____||____||____||____||____||____|");
        System.out.println("                                                            ");
        for (String outputLine : outputLines) {
            System.out.println(outputLine);
        }
        System.out.println(" ____  ____  ____  ____  ____  ____  ____  ____  ____  ____ ");
        System.out.println("|    ||    ||    ||    ||    ||    ||    ||    ||    ||    |");
        System.out.println("| P  || R  || E  || T  || T  || I  || F  || I  || E  || R  |");
        System.out.println("|____||____||____||____||____||____||____||____||____||____|");
        System.out.println("                                                            ");

        return outputLines;
    }

    public String convertCodes(String input, Map<String, String[]> airportData) {
        Map<String, String> iataMap = new HashMap<>();
        Map<String, String> icaoMap = new HashMap<>();
        Map<String, String> iataCityMap = new HashMap<>();
        Map<String, String> icaoCityMap = new HashMap<>();

        for (Map.Entry<String, String[]> entry : airportData.entrySet()) {
            String code = entry.getKey();
            String[] values = entry.getValue();
            String airportName = values[0];
            String cityName = values[1];

            if (code.length() == 3) {
                iataMap.put(code, airportName);
                iataCityMap.put(code, cityName);
            } else if (code.length() == 4) {
                icaoMap.put(code, airportName);
                icaoCityMap.put(code, cityName);
            }
        }

        StringBuilder result = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length;) {
            if (input.charAt(i) == '#') {
                if (i + 1 < length && input.charAt(i + 1) == '#') {
                    i += 2;
                    String code = extractCode(input, i);
                    result.append(getAirportAndCount(code, "##", icaoMap, icaoCityMap));
                    i += code.length();
                } else {
                    i++;
                    String code = extractCode(input, i);
                    result.append(getAirportAndCount(code, "#", iataMap, iataCityMap));
                    i += code.length();
                }
            } else if (input.charAt(i) == '*' && i + 1 < length && input.charAt(i + 1) == '#') {
                if (i + 2 < length && input.charAt(i + 2) == '#') {
                    i += 3;
                    String code = extractCode(input, i);
                    result.append(getAirportAndCount(code, "*##", icaoMap, icaoCityMap));
                    i += code.length();
                } else {
                    i += 2;
                    String code = extractCode(input, i);
                    result.append(getAirportAndCount(code, "*#", iataMap, iataCityMap));
                    i += code.length();
                }
            } else {
                result.append(input.charAt(i));
                i++;
            }
        }

        return result.toString();
    }

    private static String getAirportAndCount(String code, String mask, Map<String, String> map,
            Map<String, String> cityMap) {
        String replacement = null;
        if (mask.equals("##") || mask.equals("#")) {
            replacement = map.getOrDefault(code, null);
        } else if (mask.equals("*##") || mask.equals("*#")) {
            replacement = cityMap.getOrDefault(code, null);
        }

        return replacement != null ? replacement : (mask + code);
    }

    private static String extractCode(String input, int startIndex) {
        StringBuilder code = new StringBuilder();
        while (startIndex < input.length() && Character.isLetterOrDigit(input.charAt(startIndex))) {
            code.append(input.charAt(startIndex));
            startIndex++;
        }
        return code.toString();
    }

    private static String convertDatesAndTimes(String line) {
        Pattern datePattern = Pattern.compile("D\\(([^)]+)\\)");
        Matcher dateMatcher = datePattern.matcher(line);
        StringBuffer dateResult = new StringBuffer();

        while (dateMatcher.find()) {
            String isoDate = dateMatcher.group(1);
            try {
                OffsetDateTime dateTime = OffsetDateTime.parse(isoDate);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
                String formatterDate = dateTime.format(formatter);
                dateMatcher.appendReplacement(dateResult, formatterDate);
            } catch (DateTimeParseException e) {
                dateMatcher.appendReplacement(dateResult, dateMatcher.group());
            }
        }
        dateMatcher.appendTail(dateResult);

        Pattern time12Pattern = Pattern.compile("T12\\(([^)]+)\\)");
        Matcher time12Matcher = time12Pattern.matcher(dateResult.toString());
        StringBuffer time12Result = new StringBuffer();

        while (time12Matcher.find()) {
            String isoDate = time12Matcher.group(1);
            try {
                OffsetDateTime dateTime = OffsetDateTime.parse(isoDate);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mma (xxx)");
                String formattedTime = dateTime.format(formatter);
                time12Matcher.appendReplacement(time12Result, formattedTime);
            } catch (DateTimeParseException e) {
                time12Matcher.appendReplacement(time12Result, time12Matcher.group());
            }
        }
        time12Matcher.appendTail(time12Result);

        Pattern time24Pattern = Pattern.compile("T24\\(([^)]+)\\)");
        Matcher time24Matcher = time24Pattern.matcher(time12Result.toString());
        StringBuffer time24Result = new StringBuffer();

        while (time24Matcher.find()) {
            String isoDate = time24Matcher.group(1);
            try {
                OffsetDateTime dateTime = OffsetDateTime.parse(isoDate);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm (xxx)");
                String formattedTime = dateTime.format(formatter);
                time24Matcher.appendReplacement(time24Result, formattedTime);
            } catch (DateTimeParseException e) {
                time24Matcher.appendReplacement(time24Result, time24Matcher.group());
            }
        }
        time24Matcher.appendTail(time24Result);

        return time24Result.toString();
    }

    private static String convertWhiteSpace(String content) {
        content = content.replace("\\v", "\u000B");
        content = content.replace("\\f", "\f");
        content = content.replace("\\r", "\r");

        content = content.replaceAll("[\u000B\f\r]", "\n");
        content = content.replaceAll("\n{3,}", "");

        return content;
    }

    private static void printUsage() {
        System.out.println(GREEN + "itinerary usage:" + RESET);
        System.out.println(YELLOW + "java Prettifier.java ./input.txt ./output.txt ./airport-lookup.csv" + RESET);
    }
}
