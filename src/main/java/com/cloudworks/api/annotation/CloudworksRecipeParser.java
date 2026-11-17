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
package com.cloudworks.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CloudWorks recipe parser enable annotation.
 *
 * CloudWorks 閰嶆柟瑙ｆ瀽鍣ㄥ惎鐢ㄦ敞瑙ｃ€?
 * <p>
 * 灏嗘娉ㄨВ娣诲姞鍒颁换浣曡 NeoForge 鎵弿鐨勭被涓婏紝鍗冲彲鍚敤
 * RecipeParser 妯″潡銆傛ā鍧楀垵濮嬪寲鏃朵細鑷姩妫€娴嬫娉ㄨВ鐨勫瓨鍦ㄣ€?
 * 濡傛灉鏈娴嬪埌娉ㄨВ锛孯ecipeParser 妯″潡灏嗕繚鎸佺鐢ㄧ姸鎬併€?
 * </p>
 *
 * <p>
 * 浣跨敤绀轰緥锛?
 * <pre>
 * {@code @CloudworksRecipeParser}
 * public class MyMod {
 *     // ...
 * }
 * </pre>
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CloudworksRecipeParser {
}