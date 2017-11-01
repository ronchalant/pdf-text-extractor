package com.ronchalant.pdftextcleaner.web;

import com.ronchalant.pdftextcleaner.services.PDFTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * Created by RRRUDY on 11/1/2017.
 */
@Controller
public class CleaningController {
    @Autowired
    PDFTextExtractor pdfTextExtractor;

//    @GetMapping(path = "/")
//    public String index(Model model) {
//        return "upload";
//    }

    @PostMapping(path = "/upload", produces = "text/plain")
    public @ResponseBody String fileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) throws IOException {
        byte[] bytes = file.getBytes();
        return pdfTextExtractor.extract(bytes);
    }
}
