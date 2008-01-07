/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation the rights to 
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies 
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE.
 */
package org.joni.encoding.specific;

import org.joni.Config;
import org.joni.IntHolder;
import org.joni.encoding.unicode.UnicodeEncoding;

public final class UTF16LEEncoding extends UnicodeEncoding {

    protected UTF16LEEncoding() {
        super(UTF16BEEncoding.UTF16EncLen);
    }

    @Override
    public int length(byte c) { 
        return EncLen[(c & 0xff) + 1];       
    }    
    
    @Override
    public String toString() {
        return "UTF-16LE";
    }
    
    @Override
    public int maxLength() {
        return 4;
    }
    
    @Override
    public int minLength() {
        return 2;
    }
    
    @Override
    public boolean isFixedWidth() {
        return false;
    }
    
    @Override
    public boolean isNewLine(byte[]bytes, int p, int end) {
        if (p + 1 < end) {
            if (bytes[p] == (byte)0x0a && bytes[p + 1] == (byte)0x00) return true;
            
            if (Config.USE_UNICODE_ALL_LINE_TERMINATORS) {
                if ((!Config.USE_CRNL_AS_LINE_TERMINATOR && bytes[p] == (byte)0x0d) ||
                        bytes[p] == (byte)0x85 && bytes[p + 1] == (byte)0x00) return true;
                
                if (bytes[p + 1] == (byte)0x20 && (bytes[p] == (byte)0x29 || bytes[p] == (byte)0x28)) return true;
            }
        }
        return false;
    }
    
    private static boolean isSurrogateFirst(int c) {
        return c >= 0xd8 && c <= 0xdb;
    }
    
    @Override
    public int mbcToCode(byte[]bytes, int p, int end) {
        int code;
        if (isSurrogateFirst(bytes[p + 1] & 0xff)) {
            code = ((((bytes[p + 1] & 0xff - 0xd8) << 2) +
                     ((bytes[p + 0] & 0xff & 0xc0) >> 6) + 1) << 16) +
                   ((((bytes[p + 0] & 0xff & 0x3f) << 2) +
                      (bytes[p + 2] & 0xff - 0xdc)) << 8) +
                       bytes[p + 3] & 0xff;
        } else {
            code =  (bytes[p + 1] & 0xff) * 256 + (bytes[p + 0] & 0xff);
        }
        return code;
    }
    
    @Override
    public int codeToMbcLength(int code) {
        return code > 0xffff ? 4 : 2; 
    }
    
    @Override    
    public int codeToMbc(int code, byte[]bytes, int p) {    
        int p_ = p;
        if (code > 0xffff) {
            int plane = code >>> 16;
            int high = (code & 0xff00) >>> 8;
            bytes[p_++] = (byte)(((plane & 0x03) << 6) + (high >>> 2));
            bytes[p_++] = (byte)((plane >>> 2) + 0xd8);
            bytes[p_++] = (byte)(code & 0xff);
            bytes[p_  ] = (byte)((high & 0x02) + 0xdc);
            return 4;
        } else {
            bytes[p_++] = (byte)(code & 0xff);
            bytes[p_++] = (byte)((code & 0xff00) >>> 8);
            return 2;
        }
    }
    
    @Override
    public int mbcCaseFold(int flag, byte[]bytes, IntHolder pp, int end, byte[]fold) {
        int p = pp.value;
        int foldP = 0;

        if (isAscii(bytes[p] & 0xff) && bytes[p + 1] == 0) {

            if (Config.USE_UNICODE_CASE_FOLD_TURKISH_AZERI) {
                if ((flag & Config.ENC_CASE_FOLD_TURKISH_AZERI) != 0) {
                    if (bytes[p] == (byte)0x49) {
                        fold[foldP++] = (byte)0x01;
                        fold[foldP] = (byte)0x31;
                        pp.value += 2;
                        return 2;
                    }
                }
            } // USE_UNICODE_CASE_FOLD_TURKISH_AZERI            
            
            fold[foldP++] = ASCIIEncoding.AsciiToLowerCaseTable[bytes[p] & 0xff];
            fold[foldP] = 0;
            pp.value += 2;
            return 2;
        } else {
            return super.mbcCaseFold(flag, bytes, pp, end, fold);
        }
    }

    /** onigenc_utf16_32_get_ctype_code_range
     */
    @Override
    public int[]ctypeCodeRange(int ctype, IntHolder sbOut) {
        sbOut.value = 0x00;
        return super.ctypeCodeRange(ctype);
    }
    
    private static boolean isSurrogateSecond(int c) {
        return c >= 0xdc && c <= 0xdf;
    }    
    
    @Override
    public int leftAdjustCharHead(byte[]bytes, int p, int end) {
        if (end <= p) return end;
        
        if ((end - p) % 2 == 1) end--;
        
        if (isSurrogateSecond(bytes[end + 1] & 0xff) && end > p + 1) end -= 2;
        
        return end;
    }
    
    @Override
    public boolean isReverseMatchAllowed(byte[]bytes, int p, int end) {
        return false;
    }
    
    public static final UTF16LEEncoding INSTANCE = new UTF16LEEncoding();
}