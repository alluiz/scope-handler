package com.company.scopehandler.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class InputResolverService {
    public List<String> resolve(List<String> inlineValues, Path file) {
        Set<String> values = new LinkedHashSet<>();

        if (inlineValues != null) {
            for (String value : inlineValues) {
                addIfValid(values, value);
            }
        }

        if (file != null) {
            values.addAll(readLines(file));
        }

        if (values.isEmpty()) {
            throw new IllegalArgumentException("No values provided");
        }

        return new ArrayList<>(values);
    }

    private void addIfValid(Set<String> values, String raw) {
        if (raw == null) {
            return;
        }
        String value = raw.trim();
        if (value.isBlank()) {
            return;
        }
        values.add(value);
    }

    private List<String> readLines(Path file) {
        try {
            List<String> lines = new ArrayList<>();
            try (var stream = Files.lines(file)) {
                stream.forEach(line -> {
                    String trimmed = line.trim();
                    if (trimmed.isBlank() || trimmed.startsWith("#")) {
                        return;
                    }
                    lines.add(trimmed);
                });
            }
            return lines;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read file: " + file, e);
        }
    }
}
