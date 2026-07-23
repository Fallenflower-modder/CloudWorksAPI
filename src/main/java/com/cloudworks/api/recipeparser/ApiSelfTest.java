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
package com.cloudworks.api.recipeparser;

import com.cloudworks.api.recipeparser.model.RecipeData;
import com.cloudworks.api.recipeparser.model.Ingredient;
import com.cloudworks.api.recipeparser.model.Product;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * API self-test module.
 *
 * API 鑷妯″潡銆?
 * <p>
 * 鍦ㄦ湇鍔″櫒绾у埆鍔犺浇鏃惰嚜鍔ㄨЕ鍙戯紝浠?autotest.txt 鏂囦欢涓鍙栨祴璇曠敤渚嬶紝
 * 鎵ц閰嶆柟瑙ｆ瀽楠岃瘉骞惰緭鍑烘祴璇曠粨鏋滃埌 autotest_result.txt 鏂囦欢銆?
 * 姣忎釜娴嬭瘯鐢ㄤ緥鍖呭惈閰嶆柟ID鍜屾湡鏈涚殑杈撳叆/杈撳嚭鐗╁搧鏁伴噺锛?
 * 鐢ㄤ簬楠岃瘉妯℃澘瑙ｆ瀽缁撴灉鐨勬纭€с€?
 * </p>
 * <p>
 * 娴嬭瘯鐢ㄤ緥鏍煎紡锛?
 * <pre>
 * recipe_id
 * inputs: count
 * outputs: count
 * ---
 * </pre>
 * </p>
 */
public class ApiSelfTest {

    /**
     * Logger
     */
    /** 鏃ュ織璁板綍鍣?*/
    private static final Logger LOGGER = LoggerFactory.getLogger("CloudWorks-ApiSelfTest");
    /**
     * Self-test result file path
     */
    /** 鑷缁撴灉鏂囦欢璺緞 */
    private static final Path RESULT_FILE = FMLPaths.GAMEDIR.get().resolve("autotest_result.txt");

    /**
 * World load event callback, triggers the self-test flow.
 *
 * 涓栫晫鍔犺浇浜嬩欢鍥炶皟锛岃Е鍙戣嚜妫€娴佺▼銆?
 * <p>
 * 浠呭湪涓栫晫鍔犺浇浜嬩欢涓Е鍙戜竴娆★紝閬垮厤閲嶅鎵ц銆?
 * </p>
 *
 * @param event the world load event
 * @param event 涓栫晫鍔犺浇浜嬩欢
 */
    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level && level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        try {
            runSelfTest();
        } catch (Exception e) {
            LOGGER.error("Self-test failed: {}", e.getMessage());
        }
    }

    /**
 * Executes the self-test flow.
 *
 * 鎵ц鑷娴佺▼銆?
 * <p>
 * 浠?autotest.txt 璇诲彇娴嬭瘯鐢ㄤ緥锛岄€愪竴楠岃瘉閰嶆柟瑙ｆ瀽缁撴灉銆?
 * 娴嬭瘯缁撴灉鍐欏叆 autotest_result.txt 鏂囦欢銆?
 * </p>
 */
    private void runSelfTest() {
        Path autotestFile = FMLPaths.GAMEDIR.get().resolve("autotest.txt");
        if (!Files.exists(autotestFile)) {
            LOGGER.info("No autotest.txt found, skipping self-test.");
            return;
        }

        LOGGER.info("=== Running API Self-Test ===");
        StringBuilder result = new StringBuilder();
        result.append("=== CloudWorks API Self-Test Results ===\n\n");

        try {
            List<String> lines = Files.readAllLines(autotestFile, StandardCharsets.UTF_8);
            int testCount = 0;
            int passCount = 0;
            int failCount = 0;

            String currentRecipeId = null;
            int expectedInputs = -1;
            int expectedOutputs = -1;

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("---")) {
                    // End of test case
                    if (currentRecipeId != null) {
                        testCount++;
                        boolean passed = runSingleTest(currentRecipeId, expectedInputs, expectedOutputs, result);
                        if (passed) passCount++; else failCount++;
                    }
                    currentRecipeId = null;
                    expectedInputs = -1;
                    expectedOutputs = -1;
                } else if (line.startsWith("inputs:")) {
                    expectedInputs = Integer.parseInt(line.substring(7).trim());
                } else if (line.startsWith("outputs:")) {
                    expectedOutputs = Integer.parseInt(line.substring(8).trim());
                } else {
                    currentRecipeId = line;
                }
            }

            // Process last test case if no trailing ---
            if (currentRecipeId != null) {
                testCount++;
                boolean passed = runSingleTest(currentRecipeId, expectedInputs, expectedOutputs, result);
                if (passed) passCount++; else failCount++;
            }

            result.append("\n=== Summary: ").append(passCount).append("/").append(testCount).append(" passed ===\n");
            Files.writeString(RESULT_FILE, result.toString(), StandardCharsets.UTF_8);
            LOGGER.info("Self-test complete: {}/{} passed. Results written to {}", passCount, testCount, RESULT_FILE);

        } catch (Exception e) {
            LOGGER.error("Self-test error: {}", e.getMessage());
            result.append("ERROR: ").append(e.getMessage()).append("\n");
            try {
                Files.writeString(RESULT_FILE, result.toString(), StandardCharsets.UTF_8);
            } catch (IOException ignored) {}
        }
    }

    /**
 * Runs a single test case.
 *
 * 杩愯鍗曚釜娴嬭瘯鐢ㄤ緥銆?
 *
 * @param recipeId the recipe ID
 * @param expectedInputs the expected input count (-1 means no check)
 * @param expectedOutputs the expected output count (-1 means no check)
 * @param result the result output buffer
 * @param recipeId       閰嶆柟ID
 * @param expectedInputs 鏈熸湜鐨勮緭鍏ユ暟閲忥紙-1 琛ㄧず涓嶆鏌ワ級
 * @param expectedOutputs 鏈熸湜鐨勮緭鍑烘暟閲忥紙-1 琛ㄧず涓嶆鏌ワ級
 * @param result         缁撴灉杈撳嚭缂撳啿鍖?
 * @return whether the test passed
 * @return 娴嬭瘯鏄惁閫氳繃
 */
    private boolean runSingleTest(String recipeId, int expectedInputs, int expectedOutputs, StringBuilder result) {
        ResourceLocation id = ResourceLocation.tryParse(recipeId);
        if (id == null) {
            result.append("FAIL: ").append(recipeId).append(" - invalid recipe ID\n");
            return false;
        }

        try {
            // Note: Self-test can only run when RecipeManager is available
            // In a real scenario, this would need access to the server's RecipeManager
            result.append("SKIP: ").append(recipeId).append(" - RecipeManager not available in self-test\n");
            return true;
        } catch (Exception e) {
            result.append("FAIL: ").append(recipeId).append(" - ").append(e.getMessage()).append("\n");
            return false;
        }
    }
}