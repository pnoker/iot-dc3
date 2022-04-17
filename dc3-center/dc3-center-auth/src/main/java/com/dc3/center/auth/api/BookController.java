package com.dc3.center.auth.api;

import com.dc3.center.auth.bean.Book;
import com.dc3.center.auth.mapper.ESBookRepository;
import com.dc3.common.bean.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Optional;

@RestController
public class BookController {
    @Resource
    private ESBookRepository esBookRepository;

    @PostMapping("/book")
    public R addBook(@RequestBody Book book) {
        esBookRepository.save(book);
        return R.ok();
    }

    @GetMapping("/book/search")
    public R<Book> search(String key) {
        Optional<Book> byId = esBookRepository.findById(key);
        return R.ok(byId.get());
    }
}