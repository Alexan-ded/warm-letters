/*
 * Copyright (C) 2019 Chan Chung Kwong
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.example.offline.extractor.orderer;
import org.example.online.TraceList;
/**
 * Stroke order normalization
 *
 * @author Chan Chung Kwong
 */
public interface Orderer{
	/**
	 * Normalize stroke order
	 *
	 * @param traceList strokes
	 * @return ordered
	 */
	TraceList order(TraceList traceList);
}
