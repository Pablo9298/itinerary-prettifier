# Itinerary Prettifier

This is a command-line tool that processes flight itineraries, making them more customer-friendly by formatting airport codes, dates, times, and vertical whitespace. It reads an input text file, processes the content, and writes the result to an output file.

## Features

1. **Airport Code Conversion**:
* Converts IATA (e.g., `#LAX`) and ICAO (e.g., `##EGLL`) codes into airport names using an airport lookup CSV file.
   
2. **Date and Time Formatting**:
* Formats dates from the ISO 8601 format to `DD MMM YYYY` format.
* Formats times into either a 12-hour clock with AM/PM or a 24-hour clock with timezone offsets.

3. **Whitespace Handling**:
* Replaces vertical whitespace characters (`\v`, `\f`, `\r`) and string literals (`\\n`, `\\v`, `\\f`, `\\r`) with newlines (`\n`).
* Trims excessive blank lines to ensure no more than one blank line between paragraphs.

## Requirements

- Java 11 or higher.
- A valid input text file with the itinerary information.
- A valid airport lookup CSV file with the following columns:
  - `name`
  - `iso_country`
  - `municipality`
  - `icao_code`
  - `iata_code`
  - `coordinates`

## Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/itinerary-prettifier.git
   ```

2. Navigate into the project directory:
    ```bash
   cd itinerary-prettifier
   ```

3. Compile the project:
    ```bash
   javac Prettifier.java
   ```

## Usage

The program is executed from the command line with three arguments: the input file, the output file, and the airport lookup CSV file.

### Example Command
    
   ```
   java Prettifier ./input.txt ./output.txt ./airport-lookup.csv.
   ```

### Command-line Arguments

* `input.txt`: Path to the input file containing the raw itinerary data.
* `output.txt`: Path to the output file where the prettified itinerary will be saved.
* `airport-lookup.csv`: Path to the CSV file containing the airport lookup data.


### Flags

* `-h`: Displays the help information with usage instructions.


```bash 
java Prettifier -h
```

## Input File Format

The input file should be a plain text file containing the raw itinerary data with the following possible elements:

1.**Airport Codes:**

 * IATA codes prefixed with `#` (e.g., `#LAX`).
* ICAO codes prefixed with `##` (e.g., `##EGLL`).

2.**Dates and Times:**

* Dates in the format `D(YYYY-MM-DDTHH:MM±HH:MM)` (e.g., `D(2022-05-09T08:07Z)`).
* 12-hour times in the format `T12(YYYY-MM-DDTHH:MM±HH:MM)` (e.g., `T12(2022-05-09T08:07Z)`).
* 24-hour times in the format `T24(YYYY-MM-DDTHH:MM±HH:MM)` (e.g., `T24(2022-05-09T08:07Z)`).

2.**Vertical Whitespace:**

* Characters such as `\v`, `\f`, `\r` will be replaced by `\n`.

### Example Input File

```bash
Your flight departs from #LAX on D(2022-05-09T08:07Z).
You will land at ##EGLL at T24(2022-05-09T16:07+01:00).
Your flight departs from #HAJ, and your destination is ##EDDW.
Enjoy your trip!
```

### Output File Format

The output file will be a more customer-friendly version of the input file with formatted airport names, dates, times, and cleaned-up whitespace.

#### Example Output File

```bash
Your flight departs from Los Angeles International Airport on 09 May 2022.
You will land at London Heathrow Airport at 16:07 (+01:00).
Your flight departs from Hannover Airport, and your destination is Bremen Airport.
Enjoy your trip!
```

## CSV File Format

The airport lookup CSV file should have the following columns:

* **name**: The full name of the airport.
* **iso_country**: The ISO country code where the airport is located.
* **municipality**: The city or municipality where the airport is located.
* **icao_code**: The ICAO code of the airport.
* **iata_code**: The IATA code of the airport.
* **coordinates**: The latitude and longitude of the airport.

### Example CSV File

```bash
name,iso_country,municipality,icao_code,iata_code,coordinates
Los Angeles International Airport,US,Los Angeles,KLAX,LAX,"33.9425,-118.4081"
London Heathrow Airport,GB,London,EGLL,LHR,"51.4775,-0.461389"
Hannover Airport,DE,Hannover,EDDV,HAJ,"52.461,9.685"
Bremen Airport,DE,Bremen,EDDW,BRE,"53.0475,8.786667"
```

## Error Handling

1. **Input Not Found**: Displays "Input not found" if the input file is missing.
2. **Airport Lookup Not Found**: Displays "Airport lookup not found" if the CSV file is missing.
3. **Airport Lookup Malformed**: Displays "Airport lookup malformed" if the CSV file is missing columns or has malformed data.
4. **Incorrect Command Usage**: If the incorrect number of arguments is provided, or if the `-h` flag is used, the program will display the usage information.
