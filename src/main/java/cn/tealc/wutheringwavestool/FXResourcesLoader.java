/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of MaterialFX (https://github.com/palexdev/MaterialFX).
 *
 * MaterialFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MaterialFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package cn.tealc.wutheringwavestool;

import java.io.InputStream;
import java.net.URL;


public class FXResourcesLoader {

	private FXResourcesLoader() {
	}

	public static URL loadURL(String path) {
		return FXResourcesLoader.class.getResource(path);
	}

	public static String load(String path) {
		return loadURL(path).toExternalForm();
	}

	public static InputStream loadStream(String name) {
		return FXResourcesLoader.class.getResourceAsStream(name);
	}

}
