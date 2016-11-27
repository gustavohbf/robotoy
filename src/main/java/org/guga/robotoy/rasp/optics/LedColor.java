/*******************************************************************************
 * Copyright 2016 See https://github.com/gustavohbf/robotoy/blob/master/AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.guga.robotoy.rasp.optics;

/**
 * This enumeration lists some colors that can be used with
 * RGB Led.<BR>
 * Note: some of these 'colors' may not be displayed as expected
 * (e.g. the 'gray' ones). 
 * @author Gustavo Figueiredo
 *
 */
public enum LedColor {
	OFF("",0.0f,0.0f,0.0f),
	GREEN("#00FF00",0.0f,1.0f,0.0f),
	RED("#FF0000",1.0f,0.0f,0.0f),
	BLUE("#0000FF",0.0f,0.0f,1.0f),
	MAGENTA("#FF00FF",1.0f,0.0f,1.0f),
	CYAN("#00FFFF",0.0f,1.0f,1.0f),
	YELLOW("#FFFF00",1.0f,1.0f,0.0f),
	WHITE("#FFFFFF",1.0f,1.0f,1.0f),
	ORANGE("#FF8C00",1.0f,0.3f,0.0f),
	PINK("#FF008C",1.0f,0.0f,0.3f),
	GRAY("#8C8C8C",0.3f,0.3f,0.3f),
	DARK_GREEN("#008C00",0.0f,0.3f,0.0f),
	DARK_RED("#8C0000",0.3f,0.0f,0.0f),
	DARK_BLUE("#00008C",0.0f,0.0f,0.3f),
	DARKER_GREEN("#004C00",0.0f,0.1f,0.0f),
	DARKER_RED("#4C0000",0.1f,0.0f,0.0f),
	DARKER_BLUE("#00004C",0.0f,0.0f,0.1f);
	
	private final float r, g, b;
	private final String name;
	LedColor(String name,float r,float g,float b) {
		this.name = name;
		this.r = r;
		this.g = g;
		this.b = b;
	}
	public String getName() {
		return name;
	}
	public float getRed() {
		return r;
	}
	public float getGreen() {
		return g;
	}
	public float getBlue() {
		return b;
	}
	public LedColor next() {
		LedColor[] values = values();
		return values[(ordinal()+1)%values.length];
	}
	public static LedColor get(String name) {
		if (name==null)
			return null;
		for (LedColor color:values()) {
			if (name.equalsIgnoreCase(color.getName()))
				return color;
		}
		return null;
	}
}
