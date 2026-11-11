package com.example.demo.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.model.domain.Post;
import com.example.demo.model.dto.PostForm;
import com.example.demo.model.service.CommentService;
import com.example.demo.model.service.PostService;

@Controller // 게시판 화면 컨트롤러
@RequestMapping("/board") // 모든 URL 앞에 /board
public class BoardController {
    @Autowired
    PostService postService;
    @Autowired // ① 필드 추가 (postService 아래)
    CommentService commentService;

    // @GetMapping // 목록 : GET /board (누구나)
    // public String list(Model model) {
    // List<Post> posts = postService.findAll();
    // model.addAttribute("posts", posts);
    // return "board/list";
    // }

    @GetMapping // 목록 : GET /board?page=0&keyword=스프링 (누구나)
    public String list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword, Model model) {
        Page<Post> posts = postService.findPage(keyword, page);
        model.addAttribute("posts", posts.getContent());
        model.addAttribute("page", posts);
        model.addAttribute("keyword", keyword); // 검색창에 검색어 유지, 페이지 링크에도 붙인다
        return "board/list";
    }

    @GetMapping("/my") // [연습문제 ②] 내가 쓴 글 : GET /board/my (로그인 필요)
    public String myPosts(Principal principal, Model model) {
        model.addAttribute("posts", postService.findMyPosts(principal.getName()));
        model.addAttribute("listTitle", "내가 쓴 글"); // 목록 화면 재사용, 제목만 변경
        return "board/list";
    }

    @GetMapping("/write") // 글쓰기 화면 : GET /board/write (로그인 필요)
    public String writeForm() {
        return "board/form";
    }

    @PostMapping("/write") // 글쓰기 처리 : POST /board/write
    public String write(PostForm postForm, Principal principal, Model model) {
        try {
            Post post = postService.write(postForm, principal.getName()); // 작성자 = 로그인한 사람
            return "redirect:/board/" + post.getId(); // 저장 후 상세 화면으로
        } catch (IllegalArgumentException e) { // 빈 제목·내용
            model.addAttribute("error", e.getMessage());
            return "board/form"; // 입력값 유지한 채 다시 글쓰기 화면
        }
    }

    @GetMapping("/{id}") // 상세 : GET /board/3 (누구나)
    public String detail(@PathVariable Long id, Model model) {
        Post post = postService.read(id); // [연습문제 ①] 조회수 1 증가 후 글 1개
        model.addAttribute("post", post);
        model.addAttribute("comments", commentService.findByPost(post)); // ② 댓글 목록 → 화면에 댓글 영역
        return "board/detail";
    }

    @GetMapping("/{id}/edit") // 수정 화면 : GET /board/3/edit
    public String editForm(@PathVariable Long id, Model model) {
        Post post = postService.findForEdit(id);
        PostForm postForm = new PostForm(); // 기존 내용을 폼에 채워서 보여 준다
        postForm.setTitle(post.getTitle());
        postForm.setContent(post.getContent());
        model.addAttribute("postForm", postForm);
        model.addAttribute("postId", id); // 화면이 "수정" 모드로 바뀐다
        return "board/form";
    }

    @PostMapping("/{id}/edit") // 수정 처리 : POST /board/3/edit
    public String edit(@PathVariable Long id, PostForm postForm, Model model) {
        try {
            postService.edit(id, postForm);
            return "redirect:/board/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("postId", id);
            return "board/form";
        }
    }

    @PostMapping("/{id}/delete") // 삭제 처리 : POST /board/3/delete
    public String delete(@PathVariable Long id) {
        postService.delete(id);
        return "redirect:/board?deleted"; // 목록 + "삭제되었습니다"
    }

}