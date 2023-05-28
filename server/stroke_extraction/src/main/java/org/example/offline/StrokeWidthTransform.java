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
package org.example.offline;

import org.example.offline.preprocessor.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Stroke width transformation
 *
 * @author Chan Chung Kwong
 */
public class StrokeWidthTransform{
	public static final byte HORIZONTAL=1, VERTICAL=2, THROW=4, PRESS=8;
	/**
	 * Transform a bitmap image
	 *
	 * @param bitmap to be transform
	 * @return stroke width space
	 */
	public static StrokeSpace transform(Bitmap bitmap){
		int width=bitmap.getWidth();
		int height=bitmap.getHeight();
		byte[] pixels=bitmap.getData();
		short[] thicknessH=new short[width*height];
		short[] thicknessS=new short[width*height];
		byte[] direction=new byte[width*height];
		int[] nC=new int[width];
		int[] nwC=new int[width+1];
		int[] neC=new int[width+1];
		for(int i=0, ind=0;i<height;i++){
			int wC=0;
			int nwCp=0;
			for(int j=0;j<width;j++,ind++){
				if(pixels[ind]==0){
					++wC;
					++nC[j];
					int tmp=nwC[j+1];
					nwC[j+1]=nwCp+1;
					nwCp=tmp;
					neC[j]=neC[j+1]+1;
				}else{
					if(wC>0){
						int t=wC;
						for(int k=1, ind0=ind-1;k<=t;k++,ind0--){
							if((direction[ind0]&3)==0||t<thicknessH[ind0]){
								direction[ind0]=(byte)((direction[ind0]&~HORIZONTAL)|VERTICAL);
								thicknessH[ind0]=(short)t;
							}
						}
						wC=0;
					}
					if(nC[j]>0){
						int t=nC[j];
						for(int k=1, ind0=ind-width;k<=nC[j];k++,ind0-=width){
							if((direction[ind0]&3)==0||t<thicknessH[ind0]){
								direction[ind0]=(byte)((direction[ind0]&~VERTICAL)|HORIZONTAL);
								thicknessH[ind0]=(short)t;
							}
						}
						nC[j]=0;
					}
					if(nwCp>0){
						int t=nwCp;
						int d=width+1;
						for(int k=1, ind0=ind-d;k<=t;k++,ind0-=d){
							if((direction[ind0]&12)==0||t<thicknessS[ind0]){
								direction[ind0]=(byte)((direction[ind0]&~PRESS)|THROW);
								thicknessS[ind0]=(short)t;
							}
						}
					}
					nwCp=nwC[j+1];
					nwC[j+1]=0;
					if(neC[j+1]>0){
						int t=neC[j+1];
						int d=width-1;
						for(int k=1, ind0=ind-d;k<=t;k++,ind0-=d){
							if((direction[ind0]&12)==0||t<thicknessS[ind0]){
								direction[ind0]=(byte)((direction[ind0]&~THROW)|PRESS);
								thicknessS[ind0]=(short)t;
							}
						}
					}
					neC[j]=0;
				}
			}
		}
		return new StrokeSpace(direction,thicknessH,thicknessS,width,height);
	}
	/**
	 * Space of stroke width
	 */
	public static class StrokeSpace{
		private final byte[] direction;
		private final short[] thicknessH;
		private final short[] thicknessS;
		private final int width, height;
		/**
		 * Create a space of stroke width
		 *
		 * @param direction direction(bit or)
		 * @param thicknessH thickness in horizontal or vertical direction
		 * @param thicknessS thickness in throwing or pressing direction
		 * @param width width of the image
		 * @param height height of the image
		 */
		public StrokeSpace(byte[] direction,short[] thicknessH,short[] thicknessS,int width,int height){
			this.direction=direction;
			this.thicknessH=thicknessH;
			this.thicknessS=thicknessS;
			this.width=width;
			this.height=height;
		}
		/**
		 *
		 * @return direction(bit or)
		 */
		public byte[] getDirection(){
			return direction;
		}
		/**
		 *
		 * @return thickness in horizontal or vertical direction
		 */
		public short[] getThicknessH(){
			return thicknessH;
		}
		/**
		 *
		 * @return thickness in throwing or pressing direction
		 */
		public short[] getThicknessS(){
			return thicknessS;
		}
		/**
		 *
		 * @return width of the image
		 */
		public int getWidth(){
			return width;
		}
		/**
		 *
		 * @return height of the image
		 */
		public int getHeight(){
			return height;
		}
	}
}
