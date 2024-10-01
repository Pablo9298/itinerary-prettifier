package prettifier;

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

        Map<String, String> airportData = loadAirportLookup(airportLookupFilePath);
        if (airportData == null) {
            System.out.println(RED + "Airport lookup malformed" + RESET);
            return;
        }

        try {
            List<String> inputLines = Files.readAllLines(Paths.get(inputFilePath));
            List<String> prettifiedLines = prettifyItinerary(inputLines, airportData);
            Files.write(Paths.get(outputFilePath), prettifiedLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Загрузка и парсинг CSV-файла с данными аэропортов
    private static Map<String, String> loadAirportLookup(String airportLookupFilePath) {
        Map<String, String> airportMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(airportLookupFilePath))) {
            String header = br.readLine(); // Чтение заголовка
            String[] columns = parseCsvLine(header);

            // Проверка, что все необходимые заголовки присутствуют
            if (!isValidHeader(columns)) {
                return null;
            }

            // Чтение остальной части файла
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                if (fields.length != 6 || hasBlank(fields)) {
                    return null;
                }
                String iataCode = fields[4];
                String icaoCode = fields[3];
                String airportName = fields[0];

                airportMap.put(iataCode, airportName);
                airportMap.put(icaoCode, airportName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return airportMap;
    }

    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                // Если запятая вне кавычек, заканчиваем поле
                fields.add(currentField.toString().trim());
                currentField.setLength(0);// Очищаем для следующего поля
            } else {
                currentField.append(c);
            }
        }

        fields.add(currentField.toString().trim());

        return fields.toArray(new String[0]);
    }

    private static boolean hasBlank(String[] fields) {
        for (String field : fields) {
            if (field == null || field.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Преобразование текста из итинерария
    private static List<String> prettifyItinerary(List<String> inputLines, Map<String, String> airportData) {
        List<String> outputLines = new ArrayList<>();
        for (String line : inputLines) {
            // Преобразуем строку, шаг за шагом
            line = convertCodesToAirportNames(line, airportData);// Конвертация кодов аэропортов
            line = convertDatesAndTimes(line); // Конвертация дат и времени
            line = convertWhiteSpace(line); // Конвертация символов пробела

            // Добавляем преобразованную строку в список outputLines
            outputLines.add(line);
        }

        return outputLines;
    }

    // Преобразование кодов IATA и ICAO в названия аэропортов
    private static String convertCodesToAirportNames(String line, Map<String, String> airportData) {
        // Обработка IATA кодов
        Pattern iataPattern = Pattern.compile("#([A-Z]{3})");
        Matcher iataMatcher = iataPattern.matcher(line);
        StringBuffer iataResult = new StringBuffer();

        while (iataMatcher.find()) {
            String code = iataMatcher.group(1);
            String replacement = airportData.getOrDefault(code, iataMatcher.group());
            iataMatcher.appendReplacement(iataResult, replacement);
        }
        iataMatcher.appendTail(iataResult);

        // Обработка ICAO кодов
        Pattern icaoPattern = Pattern.compile("##([A-Z]{4})");
        Matcher icaoMatcher = icaoPattern.matcher(iataResult.toString());
        StringBuffer icaoResult = new StringBuffer();

        while (icaoMatcher.find()) {
            String code = icaoMatcher.group(1);
            String replacement = airportData.getOrDefault(code, icaoMatcher.group());
            icaoMatcher.appendReplacement(icaoResult, replacement);
        }
        icaoMatcher.appendTail(icaoResult);

        return icaoResult.toString();
    }

    // Преобразование дат и времени в формат для клиентов
    private static String convertDatesAndTimes(String line) {
        // Преобразование форматов D(...) для дат
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
                // Если дата неверная, оставляем как есть
                dateMatcher.appendReplacement(dateResult, dateMatcher.group());
            }
        }
        dateMatcher.appendTail(dateResult);

        // Преобразование форматов T12(...) для времени 12-часового формата
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
                // Если дата неверная, оставляем как есть
                time12Matcher.appendReplacement(time12Result, time12Matcher.group());
            }
        }
        time12Matcher.appendTail(time12Result);

        // Преобразование форматов T24(...) для времени 24-часового формата
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
                // Если дата неверная, оставляем как есть
                time24Matcher.appendReplacement(time24Result, time24Matcher.group());
            }
        }
        time24Matcher.appendTail(time24Result);

        return time24Result.toString();
    }

    // Проверка корректности заголовков CSV
    private static boolean isValidHeader(String[] header) {
        // Требуемые заголовки колонок
        Set<String> requiredColumns = new HashSet<>(
                Arrays.asList("name", "iso_country", "municipality", "icao_code", "iata_code", "coordinates"));

        // Преобразуем заголовки файла в Set для удобной проверки
        Set<String> headerSet = new HashSet<>(Arrays.asList(header));

        // Проверяем, содержатся ли все обязательные колонки в заголовке
        return headerSet.containsAll(requiredColumns);
    }

    // Преобразование вертикальных символов пробела и удаление лишних пустых строк
    private static String convertWhiteSpace(String line) {
        // Используем replace вместо replaceAll, чтобы точно заменить символы, если они
        // присутствуют
        line = line.replace("\\n", "\n"); // Строковый литерал \n на новую строку
        line = line.replace("\\v", "\n"); // Строковый литерал \v на новую строку
        line = line.replace("\\f", "\n"); // Строковый литерал \f на новую строку
        line = line.replace("\\r", "\n"); // Строковый литерал \r на новую строку

        // Удаляем лишние пустые строки (ограничиваем до двух \n подряд)
        line = line.replaceAll("\\n{3,}", "\n\n");

        return line;
    }

    // Печать информации по использованию программы
    private static void printUsage() {
        System.out.println(GREEN + "itinerary usage:" + RESET);
        System.out.println(YELLOW + "java Prettifier.java ./input.txt ./output.txt ./airport-lookup.csv" + RESET);
    }
}