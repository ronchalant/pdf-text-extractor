package com.ronchalant.pdftextcleaner.services;

import com.ronchalant.pdftextcleaner.util.UnaccentUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Created by RRRUDY on 11/1/2017.
 */
@Service
public class PDFTextExtractorImpl implements PDFTextExtractor {
    static final String LINE_SEP = System.getProperty("line.separator");

    Map<Character, String> customEscapes;

    @PostConstruct
    void initCustomEscapes() {
        customEscapes = new Hashtable<>();

        // these suck. common for PDFs from Latek (sp) apparently.

        // SMALL LIGATURE FF comes in as 'dot above', U+02D9: http://www.fileformat.info/info/unicode/char/02d9/index.htm
        customEscapes.put('\u02D9', "ff");

        // SMALL LIGATURE FI comes in as 'DOUBLE ACUTE ACCENT', U+02DD: http://www.fileformat.info/info/unicode/char/02dd/index.htm
        customEscapes.put('\u02DD', "fi");

        // SMALL LIGATURE FL comes in as 'OGONEK', U+02DB: http://www.fileformat.info/info/unicode/char/02db/index.htm
        customEscapes.put('\u02DB', "fl");
    }

    public String extract(byte[] pdf) throws IOException {
        // load as pdf.
        try (PDDocument doc = PDDocument.load(pdf)) {
            // apply text cleaning to the existing content in-place first. easier to get correct
            // mappings.
            unaccentInPlace(doc);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setParagraphEnd(LINE_SEP);

            String strContent = stripper.getText(doc);
            // for good measure? is this even necessary?
            return UnaccentUtil.unaccent(strContent);
        }
    }

    String applyCustomEscapes(final String in) {
        final StringBuffer out = new StringBuffer();
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (customEscapes.containsKey(c)) {
                out.append(customEscapes.get(c));
            }
            else {
                int dec = (int)c;
                if (dec > 700) {
                    System.out.println("Found crazy character (dec: " + dec + ", hex: " + Integer.toHexString(dec) + ") at [" + i + "] of " + in);
                }
                out.append(c);
            }
        }
        return out.toString();
    }

    void unaccentInPlace(PDDocument document) throws IOException {
        PDPageTree pages = document.getDocumentCatalog().getPages();
        for (PDPage page : pages) {
            PDFStreamParser parser = new PDFStreamParser(page);
            parser.parse();
            List tokens = parser.getTokens();
            for (int j = 0; j < tokens.size(); j++) {
                Object next = tokens.get(j);
                if (next instanceof Operator) {
                    Operator op = (Operator) next;
                    //Tj and TJ are the two operators that display strings in a PDF
                    if (op.getName().equals("Tj")) {
                        // Tj takes one operator and that is the string to display so lets update that operator
                        COSString previous = (COSString) tokens.get(j - 1);
                        String string = previous.getString();
                        string = UnaccentUtil.unaccent(string);
                        string = applyCustomEscapes(string);
                        previous.setValue(string.getBytes());
                    } else if (op.getName().equals("TJ")) {
                        COSArray previous = (COSArray) tokens.get(j - 1);
                        for (int k = 0; k < previous.size(); k++) {
                            Object arrElement = previous.getObject(k);
                            if (arrElement instanceof COSString) {
                                COSString cosString = (COSString) arrElement;
                                String string = cosString.getString();
                                string = UnaccentUtil.unaccent(string);
                                string = applyCustomEscapes(string);
                                cosString.setValue(string.getBytes());
                            }
                        }
                    }
                }
            }
            PDStream updatedStream = new PDStream(document);
            OutputStream out = updatedStream.createOutputStream();
            ContentStreamWriter tokenWriter = new ContentStreamWriter(out);
            tokenWriter.writeTokens(tokens);
            page.setContents(updatedStream);
            out.close();
        }
    }
}
