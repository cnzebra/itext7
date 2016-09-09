/*

    This file is part of the iText (R) project.
    Copyright (c) 1998-2016 iText Group NV
    Authors: Bruno Lowagie, Paulo Soares, et al.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.io.font.otf;

import com.itextpdf.io.util.TextUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GlyphLine {
    protected List<Glyph> glyphs;
    protected List<ActualText> actualText;
    public int start;
    public int end;
    public int idx;

    public GlyphLine() {
        this.glyphs = new ArrayList<>();
    }

    public GlyphLine(List<Glyph> glyphs) {
        this.glyphs = glyphs;
        this.start = 0;
        this.end = glyphs.size();
    }

    public GlyphLine(List<Glyph> glyphs, int start, int end) {
        this.glyphs = glyphs;
        this.start = start;
        this.end = end;
    }

    protected GlyphLine(List<Glyph> glyphs, List<ActualText> actualText, int start, int end) {
        this(glyphs, start, end);
        this.actualText = actualText;
    }

    public GlyphLine(GlyphLine other) {
        this.glyphs = other.glyphs;
        this.actualText = other.actualText;
        this.start = other.start;
        this.end = other.end;
        this.idx = other.idx;
    }

    public GlyphLine(GlyphLine other, int start, int end) {
        this.glyphs = other.glyphs.subList(start, end);
        if (other.actualText != null) {
            this.actualText = other.actualText.subList(start, end);
        }
        this.start = 0;
        this.end = end - start;
        this.idx = other.idx - start;
    }

    public String toUnicodeString(int start, int end) {
        ActualTextIterator iter = new ActualTextIterator(this, start, end);
        StringBuilder str = new StringBuilder();
        while (iter.hasNext()) {
            GlyphLinePart part = iter.next();
            if (part.actualText != null) {
                str.append(part.actualText);
            } else {
                for (int i = part.start; i < part.end; i++) {
                    if (glyphs.get(i).getChars() != null) {
                        str.append(glyphs.get(i).getChars());
                    } else if (glyphs.get(i).hasValidUnicode()) {
                        str.append(TextUtil.convertFromUtf32(glyphs.get(i).getUnicode()));
                    }
                }
            }
        }
        return str.toString();
    }

    public GlyphLine copy(int left, int right) {
        GlyphLine glyphLine = new GlyphLine();
        glyphLine.start = 0;
        glyphLine.end = right - left;
        glyphLine.glyphs = new ArrayList<>(glyphs.subList(left, right));
        glyphLine.actualText = actualText == null ? null : new ArrayList<>(actualText.subList(left, right));
        return glyphLine;
    }

    public Glyph get(int index) {
        return glyphs.get(index);
    }

    public Glyph set(int index, Glyph glyph) {
        return glyphs.set(index, glyph);
    }

    public void add(Glyph glyph) {
        glyphs.add(glyph);
        if (actualText != null) {
            actualText.add(null);
        }
    }

    public void add(int index, Glyph glyph) {
        glyphs.add(index, glyph);
        if (actualText != null) {
            actualText.add(index, null);
        }
    }

    public void setGlyphs(List<Glyph> replacementGlyphs) {
        glyphs = new ArrayList<>(replacementGlyphs);
        start = 0;
        end = replacementGlyphs.size();
        actualText = null;
    }

    public void replaceContent(GlyphLine other) {
        glyphs.clear();
        glyphs.addAll(other.glyphs);
        if (actualText != null) {
            actualText.clear();
        }
        if (other.actualText != null) {
            if (actualText == null) {
                actualText = new ArrayList<>();
            }
            actualText.addAll(other.actualText);
        }
        start = other.start;
        end = other.end;
    }

    public int size() {
        return glyphs.size();
    }

    public void substituteManyToOne(OpenTypeFontTableReader tableReader, int lookupFlag, int rightPartLen, int substitutionGlyphIndex) {
        OpenTableLookup.GlyphIndexer gidx = new OpenTableLookup.GlyphIndexer();
        gidx.line = this;
        gidx.idx = idx;

        StringBuilder chars = new StringBuilder();
        Glyph currentGlyph = glyphs.get(idx);
        if (currentGlyph.getChars() != null) {
            chars.append(currentGlyph.getChars());
        } else if (currentGlyph.hasValidUnicode()) {
            chars.append(TextUtil.convertFromUtf32(currentGlyph.getUnicode()));
        }

        for (int j = 0; j < rightPartLen; ++j) {
            gidx.nextGlyph(tableReader, lookupFlag);
            currentGlyph = glyphs.get(gidx.idx);
            if (currentGlyph.getChars() != null) {
                chars.append(currentGlyph.getChars());
            } else if (currentGlyph.hasValidUnicode()) {
                chars.append(TextUtil.convertFromUtf32(currentGlyph.getUnicode()));
            }
            removeGlyph(gidx.idx--);
        }
        char[] newChars = new char[chars.length()];
        chars.getChars(0, chars.length(), newChars, 0);
        Glyph newGlyph = tableReader.getGlyph(substitutionGlyphIndex);
        newGlyph.setChars(newChars);
        glyphs.set(idx, newGlyph);
        end -= rightPartLen;
    }

    public void substituteOneToOne(OpenTypeFontTableReader tableReader, int substitutionGlyphIndex) {
        Glyph oldGlyph = glyphs.get(idx);
        Glyph newGlyph = tableReader.getGlyph(substitutionGlyphIndex);
        if (oldGlyph.getChars() != null) {
            newGlyph.setChars(oldGlyph.getChars());
        } else if (newGlyph.hasValidUnicode()) {
            newGlyph.setChars(TextUtil.convertFromUtf32(newGlyph.getUnicode()));
        } else if (oldGlyph.hasValidUnicode()) {
            newGlyph.setChars(TextUtil.convertFromUtf32(oldGlyph.getUnicode()));
        }
        glyphs.set(idx, newGlyph);
    }

    public void substituteOneToMany(OpenTypeFontTableReader tableReader, int[] substGlyphIds) {
        int substCode = substGlyphIds[0]; //sequence length shall be at least 1
        Glyph glyph = tableReader.getGlyph(substCode);
        glyphs.set(idx, glyph);

        if (substGlyphIds.length > 1) {
            List<Glyph> additionalGlyphs = new ArrayList<>(substGlyphIds.length - 1);
            for (int i = 1; i < substGlyphIds.length; ++i) {
                substCode = substGlyphIds[i];
                glyph = tableReader.getGlyph(substCode);
                additionalGlyphs.add(glyph);
            }
            addAllGlyphs(idx + 1, additionalGlyphs);
            idx += substGlyphIds.length - 1;
            end += substGlyphIds.length - 1;
        }
    }

    public GlyphLine filter(IGlyphLineFilter filter) {
        boolean anythingFiltered = false;
        List<Glyph> filteredGlyphs = new ArrayList<>(end - start);
        List<ActualText> filteredActualText = actualText != null ? new ArrayList<ActualText>(end - start) : null;
        for (int i = start; i < end; i++) {
            if (filter.accept(glyphs.get(i))) {
                filteredGlyphs.add(glyphs.get(i));
                if (filteredActualText != null) {
                    filteredActualText.add(actualText.get(i));
                }
            } else {
                anythingFiltered = true;
            }
        }
        if (anythingFiltered) {
            return new GlyphLine(filteredGlyphs, filteredActualText, 0, filteredGlyphs.size());
        } else {
            return this;
        }
    }

    public void setActualText(int left, int right, String text) {
        if (this.actualText == null) {
            this.actualText = new ArrayList<>(glyphs.size());
            for (int i = 0; i < glyphs.size(); i++)
                this.actualText.add(null);
        }
        ActualText actualText = new ActualText(text);
        for (int i = left; i < right; i++) {
            this.actualText.set(i, actualText);
        }
    }

    public Iterator<GlyphLinePart> iterator() {
        return new ActualTextIterator(this);
    }

    private void removeGlyph(int index) {
        glyphs.remove(index);
        if (actualText != null) {
            actualText.remove(index);
        }
    }

    private void addAllGlyphs(int index, List<Glyph> additionalGlyphs) {
        glyphs.addAll(index, additionalGlyphs);
        if (actualText != null) {
            for (int i = 0; i < additionalGlyphs.size(); i++) {
                this.actualText.add(index, null);
            }
        }
    }

    public static class GlyphLinePart {
        public int start;
        public int end;
        public String actualText;
        public boolean reversed;

        public GlyphLinePart(int start, int end) {
            this(start, end, null);
        }

        public GlyphLinePart(int start, int end, String actualText) {
            this.start = start;
            this.end = end;
            this.actualText = actualText;
        }

        public GlyphLinePart setReversed(boolean reversed) {
            this.reversed = reversed;
            return this;
        }
    }

    public interface IGlyphLineFilter {
        boolean accept(Glyph glyph);
    }

    protected static class ActualText {
        public ActualText(String value) {
            this.value = value;
        }

        public String value;
    }
}
