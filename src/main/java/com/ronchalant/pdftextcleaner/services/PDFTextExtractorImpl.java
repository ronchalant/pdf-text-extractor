package com.ronchalant.pdftextcleaner.services;

import com.ronchalant.pdftextcleaner.util.UnaccentUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by RRRUDY on 11/1/2017.
 */
@Service
public class PDFTextExtractorImpl implements PDFTextExtractor {
    public String extract(byte[] pdf) throws IOException {
        // load as pdf.
        try (PDDocument doc = PDDocument.load(pdf)) {
            String strContent = new PDFTextStripper().getText(doc);
            return UnaccentUtil.unaccent(strContent);
        }
    }
}
