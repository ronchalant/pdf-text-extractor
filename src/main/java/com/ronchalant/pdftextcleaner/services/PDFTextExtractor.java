package com.ronchalant.pdftextcleaner.services;

import java.io.IOException;

/**
 * Created by RRRUDY on 11/1/2017.
 */
public interface PDFTextExtractor {
    String extract(byte[] pdf) throws IOException;
}
