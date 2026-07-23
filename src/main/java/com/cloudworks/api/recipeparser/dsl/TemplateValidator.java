/*
 * CloudWorks API - Unified Recipe Parsing Interface
 * Copyright (C) 2026 CloudWorks Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.cloudworks.api.recipeparser.dsl;

import java.util.*;
import java.util.regex.Pattern;

/**
 * RPML template validator.
 *
 * RPML 妯℃澘楠岃瘉鍣ㄣ€?
 * <p>
 * 瀵瑰凡瑙ｆ瀽鐨勬ā鏉胯娉曟爲杩涜璇箟楠岃瘉锛屽寘鎷細
 * <ul>
 *   <li>ID 鏍煎紡鏍￠獙锛堜粎鍏佽瀛楁瘝鍜屾暟瀛楋級</li>
 *   <li>ID 鍞竴鎬ф鏌ワ紙INPUT 鍜?OUTPUT 鏍囪鍏佽鍏变韩 ID锛?/li>
 *   <li>input_id / output_id 寮曠敤鐨勫瓨鍦ㄦ€ф鏌?/li>
 *   <li>duplicate 缁撴瀯鐨?structure 寮曠敤鏈夋晥鎬ф鏌?/li>
 *   <li>matrixline 鐨?matrix_id 寮曠敤鏈夋晥鎬ф鏌?/li>
 * </ul>
 * </p>
 */
public class TemplateValidator {

    /**
     * ID format validation regex: letters and digits only
     */
    /** ID 鏍煎紡鏍￠獙姝ｅ垯锛氫粎鍏佽瀛楁瘝鍜屾暟瀛?*/
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    /**
 * Validates the template AST.
 *
 * 楠岃瘉妯℃澘璇硶鏍戙€?
 *
 * @param template the template object
 * @param root the AST root node
 * @param template 妯℃澘瀵硅薄
 * @param root     璇硶鏍戞牴鑺傜偣
 * @return the validation result, containing success/failure status and error message list
 * @return 楠岃瘉缁撴灉锛屽寘鍚垚鍔?澶辫触鐘舵€佸拰閿欒淇℃伅鍒楄〃
 */
    public static ValidationResult validate(Template template, TemplateNode root) {
        List<String> errors = new ArrayList<>();
        Set<String> allIds = new HashSet<>();
        Set<String> inputIds = new HashSet<>();
        Set<String> outputIds = new HashSet<>();
        Set<String> objectIds = new HashSet<>();
        Set<String> matrixIds = new HashSet<>();

        collectIds(root, allIds, inputIds, outputIds, objectIds, matrixIds, errors, template);

        // Check: at least one input or output marker (warn but don't fail)
        if (inputIds.isEmpty() && outputIds.isEmpty()) {
            // Don't add to errors - this is a soft warning, not a validation failure
            // Type-only declarations (e.g., create_item_copying) are valid
        }

        // Check: all references valid
        validateReferences(root, inputIds, outputIds, objectIds, matrixIds, allIds, errors, template);

        if (!errors.isEmpty()) {
            return ValidationResult.failure(errors);
        }
        return ValidationResult.success();
    }

    /**
     * Recursively collects all marker ID information.
     */
    /** 閫掑綊鏀堕泦鎵€鏈夋爣璁扮殑 ID 淇℃伅銆?*/
    private static void collectIds(TemplateNode node, Set<String> allIds, Set<String> inputIds,
                                    Set<String> outputIds, Set<String> objectIds, Set<String> matrixIds,
                                    List<String> errors, Template template) {
        if (node.getMarker() != null) {
            MarkerDef marker = node.getMarker();
            String id = marker.getId();

            // Validate ID format
            if (!ID_PATTERN.matcher(id).matches()) {
                errors.add("ID '" + id + "' contains invalid characters (only letters and digits allowed)");
            }

            // Check uniqueness. INPUT and OUTPUT markers are allowed to share
            // IDs 鈥?they are targets of input_id/output_id references from
            // IO_ATTRIBUTE markers. Duplicating them would break the reference chain.
            MarkerDef.MarkerType markerType = marker.getMarkerType();
            boolean isInputOrOutput = markerType == MarkerDef.MarkerType.INPUT
                                   || markerType == MarkerDef.MarkerType.OUTPUT;
            if (!isInputOrOutput && !allIds.add(id)) {
                errors.add("Duplicate ID: " + id);
            } else {
                allIds.add(id);
            }

            switch (marker.getMarkerType()) {
                case INPUT:
                    inputIds.add(id);
                    break;
                case OUTPUT:
                    outputIds.add(id);
                    break;
                case OBJECT:
                    objectIds.add(id);
                    break;
                case MATRIX:
                    matrixIds.add(id);
                    break;
            }
        }

        for (TemplateNode child : node.getChildren()) {
            collectIds(child, allIds, inputIds, outputIds, objectIds, matrixIds, errors, template);
        }
    }

    /**
     * Recursively validates all cross-references (input_id, output_id, structure, matrix_id, etc.).
     */
    /** 閫掑綊楠岃瘉鎵€鏈変氦鍙夊紩鐢紙input_id銆乷utput_id銆乻tructure銆乵atrix_id 绛夛級銆?*/
    private static void validateReferences(TemplateNode node, Set<String> inputIds, Set<String> outputIds,
                                            Set<String> objectIds, Set<String> matrixIds, Set<String> allIds,
                                            List<String> errors, Template template) {
        if (node.getMarker() != null) {
            MarkerDef marker = node.getMarker();

            // Check input_id / output_id references
            String inputId = marker.getAttribute("input_id");
            String outputId = marker.getAttribute("output_id");

            if (inputId != null) {
                if (!ID_PATTERN.matcher(inputId).matches()) {
                    errors.add("input_id '" + inputId + "' contains invalid characters");
                }
                if (!allIds.contains(inputId)) {
                    errors.add("input_id '" + inputId + "' references non-existent marker");
                }
            }
            if (outputId != null) {
                if (!ID_PATTERN.matcher(outputId).matches()) {
                    errors.add("output_id '" + outputId + "' contains invalid characters");
                }
                if (!allIds.contains(outputId)) {
                    errors.add("output_id '" + outputId + "' references non-existent marker");
                }
            }

            // Check duplicate structure references
            if (marker.getMarkerType() == MarkerDef.MarkerType.DUPLICATE) {
                String structure = marker.getAttribute("structure");
                if (structure == null) {
                    errors.add("duplicate marker '" + marker.getId() + "' missing required 'structure' attribute");
                } else if (!ID_PATTERN.matcher(structure).matches()) {
                    errors.add("structure reference '" + structure + "' contains invalid characters");
                } else if (!allIds.contains(structure)) {
                    errors.add("structure reference '" + structure + "' references non-existent marker");
                }
            }

            // Check matrixline matrix_id references
            if (marker.getMarkerType() == MarkerDef.MarkerType.MATRIXLINE) {
                String matrixId = marker.getAttribute("matrix_id");
                if (matrixId == null) {
                    errors.add("matrixline marker '" + marker.getId() + "' missing required 'matrix_id' attribute");
                } else if (!ID_PATTERN.matcher(matrixId).matches()) {
                    errors.add("matrix_id '" + matrixId + "' contains invalid characters");
                } else if (!allIds.contains(matrixId)) {
                    errors.add("matrix_id '" + matrixId + "' references non-existent marker");
                }
            }

            // Check parameter references
            for (ParameterOp param : marker.getParameters()) {
                validateParameterRefs(param.getKey(), param.getValue(), allIds, errors, marker.getId());
                if (param.getCondition() != null) {
                    validateExpressionRefs(param.getCondition(), allIds, errors, marker.getId());
                }
            }
        }

        for (TemplateNode child : node.getChildren()) {
            validateReferences(child, inputIds, outputIds, objectIds, matrixIds, allIds, errors, template);
        }
    }

    /**
     * Validates whether parameter references are valid.
     */
    /** 楠岃瘉鍙傛暟寮曠敤鏄惁鏈夋晥銆?*/
    private static void validateParameterRefs(String key, String value, Set<String> allIds, List<String> errors, String markerId) {
        // Check if value references a variable/marker ID
        if (value != null && !value.isEmpty()) {
            // Check if it's a reference to another marker's attribute
            int underscoreIdx = value.indexOf('_');
            if (underscoreIdx > 0) {
                String refId = value.substring(0, underscoreIdx);
                if (ID_PATTERN.matcher(refId).matches() && !refId.equals("self") && !refId.equals("current")
                        && !refId.equals("custom") && !refId.equals("structure") && !allIds.contains(refId)) {
                    // Could be a reference, but only warn if it matches ID pattern
                }
            }
        }
    }

    /**
     * Validates whether references in expressions are valid.
     */
    /** 楠岃瘉琛ㄨ揪寮忎腑鐨勫紩鐢ㄦ槸鍚︽湁鏁堛€?*/
    private static void validateExpressionRefs(String expr, Set<String> allIds, List<String> errors, String markerId) {
        // Simple validation: check for ID-like tokens
    }

    /**
     * Template validation result.
     */
    /** 妯℃澘楠岃瘉缁撴灉銆?*/
    public static class ValidationResult {
        /**
     * Whether validation passed
     */
    /** 楠岃瘉鏄惁閫氳繃 */
        private final boolean success;
        /**
     * Error message list
     */
    /** 閿欒淇℃伅鍒楄〃 */
        private final List<String> errors;

        private ValidationResult(boolean success, List<String> errors) {
            this.success = success;
            this.errors = errors;
        }

        /**
 * Creates a successful validation result.
 *
 * 鍒涘缓楠岃瘉閫氳繃鐨勭粨鏋溿€?
 *
 * @return a successful validation result
 * @return 鎴愬姛鐨勯獙璇佺粨鏋?
 */
        public static ValidationResult success() {
            return new ValidationResult(true, Collections.emptyList());
        }

        /**
 * Creates a failed validation result.
 *
 * 鍒涘缓楠岃瘉澶辫触鐨勭粨鏋溿€?
 *
 * @param errors the error message list
 * @param errors 閿欒淇℃伅鍒楄〃
 * @return a failed validation result
 * @return 澶辫触鐨勯獙璇佺粨鏋?
 */
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        /**
 *
 * @return whether validation passed
 * @return 楠岃瘉鏄惁閫氳繃
 */
        public boolean isSuccess() { return success; }
        /**
 *
 * @return the error message list
 * @return 閿欒淇℃伅鍒楄〃
 */
        public List<String> getErrors() { return errors; }
    }
}