

package com.example.sovereign_ai.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class StructuredDataExtractionService {

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> extract(MultipartFile file)
            throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = Optional.ofNullable(
                file.getOriginalFilename()
        ).orElse("").toLowerCase(Locale.ROOT);

        if (filename.endsWith(".csv")) {
            return extractCsv(file);
        }

        if (filename.endsWith(".xlsx")
                || filename.endsWith(".xls")) {
            return extractExcel(file);
        }

        if (filename.endsWith(".json")) {
            return extractJson(file);
        }

        throw new IllegalArgumentException(
                "Unsupported format. Use CSV, Excel, or JSON."
        );
    }

    private Map<String, Object> extractCsv(MultipartFile file)
            throws Exception {

        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns;

        try (InputStreamReader reader = new InputStreamReader(
                file.getInputStream(),
                StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .get()
                     .parse(reader)) {

            columns = new ArrayList<>(parser.getHeaderNames());

            for (CSVRecord record : parser) {
                Map<String, Object> row = new LinkedHashMap<>();

                for (String column : columns) {
                    row.put(column, record.get(column));
                }

                rows.add(row);
            }
        }

        return dataset(columns, rows);
    }

    private Map<String, Object> extractExcel(MultipartFile file)
            throws Exception {

        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();

        try (InputStream input = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(input)) {

            if (workbook.getNumberOfSheets() == 0) {
                return dataset(columns, rows);
            }

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.iterator();

            if (!iterator.hasNext()) {
                return dataset(columns, rows);
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator =
                    workbook.getCreationHelper()
                            .createFormulaEvaluator();

            Row header = iterator.next();
            int columnCount = header.getLastCellNum();

            if (columnCount <= 0) {
                return dataset(columns, rows);
            }

            for (int i = 0; i < columnCount; i++) {
                String name = formatter.formatCellValue(
                        header.getCell(i)
                ).trim();

                columns.add(name.isEmpty()
                        ? "column_" + (i + 1)
                        : name);
            }

            while (iterator.hasNext()) {
                Row excelRow = iterator.next();
                Map<String, Object> row = new LinkedHashMap<>();

                for (int i = 0; i < columns.size(); i++) {
                    Cell cell = excelRow.getCell(i);

                    String value = cell == null
                            ? ""
                            : formatter.formatCellValue(
                            cell, evaluator
                    );

                    row.put(columns.get(i), value);
                }

                rows.add(row);
            }
        }

        return dataset(columns, rows);
    }

    private Map<String, Object> extractJson(MultipartFile file)
            throws Exception {

        JsonNode root = mapper.readTree(file.getInputStream());

        if (!root.isArray()) {
            if (root.has("rows") && root.get("rows").isArray()) {
                root = root.get("rows");
            } else {
                throw new IllegalArgumentException(
                        "JSON must be an array of objects " +
                                "or contain a rows array."
                );
            }
        }

        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        for (JsonNode item : root) {
            if (!item.isObject()) {
                throw new IllegalArgumentException(
                        "Each JSON row must be an object."
                );
            }

            Map<String, Object> row =
                    mapper.convertValue(item, Map.class);

            rows.add(row);

            for (String key : row.keySet()) {
                if (!columns.contains(key)) {
                    columns.add(key);
                }
            }
        }

        return dataset(columns, rows);
    }

    private Map<String, Object> dataset(
            List<String> columns,
            List<Map<String, Object>> rows
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("rowCount", rows.size());
        return result;
    }
}
