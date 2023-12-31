package web.controller;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.ibatis.annotations.Param;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import web.dto.Board;
import web.dto.Comments;
import web.dto.Files;
import web.dto.Likes;
import web.dto.SourceLike;
import web.dto.Tag;
import web.dto.Users;
import web.service.face.BoardService;
import web.util.Paging;

@Controller
@RequestMapping("/board")
public class BoardController {
   
   @Autowired BoardService boardService;
   
   private final Logger logger = LoggerFactory.getLogger(BoardController.class);
   
   @GetMapping("/list")
   public void list( Paging paramData, Board board, Model model, String userId, String keyword, String categoryType ) {
      logger.info("board/list [GET] ❤️도착❤️");
      
      logger.info("CCCCCCCCCCCCCCCCCCCCCCategoryType {}", categoryType);
      if (categoryType == null) {
          categoryType = "all";
      }
      
      //페이징 계산
      Paging paging = boardService.getPaging(paramData, keyword, categoryType);
      
      //게시글 목록 조회
      List<Board> boardList = boardService.boardList(paging, userId, keyword, categoryType);
      
      model.addAttribute("paging", paging);
      model.addAttribute("boardList", boardList);
      model.addAttribute("userId", userId);
      model.addAttribute("keyword", keyword);
      model.addAttribute("categoryType", categoryType);
      
      logger.info("ccccccccccccccccccccccccategoryType {}", categoryType);
      logger.info("1");
   }
   
   @PostMapping("/list")
   public String listpost(
		   Paging paramData, Model model, String userId, Board board, String keyword, String categoryType,
		   @RequestParam(value="check") int[] check ) {
	   
	   logger.info("/board/list [Post]");
	   
	   //페이징 계산
	   Paging paging = boardService.getPaging(paramData, keyword, categoryType);
	   logger.info("check : {}", check);
	   
	   //게시글 목록 조회
	   List<Board> boardList = boardService.boardList(paging, userId, keyword, categoryType);
	   model.addAttribute("paging", paging);
	   model.addAttribute("boardList", boardList);
	   model.addAttribute("userId", userId);
	   model.addAttribute("categoryType", board.getCategoryType());
	   
	   //관리자일때 리스트에서 선택한거 삭제가능하게 만드는거 - 보현
	   logger.info("***check의값 : ***{}",check);
	   boardService.deleteCheckBoard(check);
	   
	   return "redirect:./list";
   }
   
   @RequestMapping("/category")
   @ResponseBody
   public List<Board> category( Paging paging, Board board, Model model, String userId, String keyword, String categoryType ) {
      logger.info("board/category [GET] ❤️도착❤️");
      
      logger.info("CCCCCCCCCCCCCCCCCCCCCCategoryType {}", categoryType);
      if (categoryType == null || "all".equals(categoryType)) {
    	  logger.info("전체 카테고리 선택!");
          categoryType = "all";
      }
      //페이징 계산
      Paging page = boardService.getPaging(paging, keyword, categoryType);
      
      //게시글 목록 조회
      List<Board> boardList = boardService.boardList(page, userId, keyword, categoryType);
      model.addAttribute("page", page);
      model.addAttribute("boardList", boardList);
      model.addAttribute("userId", userId);
      model.addAttribute("keyword", keyword);
      model.addAttribute("categoryType", board.getCategoryType());
      
      return boardList;
   }
   
   
   @RequestMapping("/view")
   public String view( Board viewBoard, Model model, List<MultipartFile> file, HttpSession session, Comments comments, Likes like ) {
      logger.info("/board/view ❤️도착❤️");
      
      //잘못된 게시글 번호 처리
      if( viewBoard.getBoardNo() < 1 ) {
         return "redirect:./list";
      }
      
      //게시글 조회
      viewBoard = boardService.view( viewBoard );
      logger.info("조회된 게시글 {}", viewBoard);
      
      //모델값 전달  - 게시글
      model.addAttribute("viewBoard", viewBoard);
      logger.info("viewBoard : {} ",viewBoard);
      
      //첨부파일 정보 모델값 전달
      List<Files> boardFile = boardService.getAttachFile(viewBoard);
      
      model.addAttribute("boardFile", boardFile);
      
      //댓글 조회
      int boardNo = viewBoard.getBoardNo();
      logger.info("boardNo {}", boardNo);
      
      List<Comments> commentList = boardService.viewComment(comments);
      model.addAttribute("commentList", commentList);
      
      // 좋아요 수 조회
      int likeCount = boardService.getBoardLikeCount(like);
      logger.info("VIEW - 좋아요 수는 ? : {}", likeCount);
      logger.info("VIEW - viewBoard.likeCount : {}", viewBoard.getLikeCount());
      model.addAttribute("likeCount", likeCount);
      
      //좋아요 이력 조회 (0이면 좋아요 안 한 상황 -> 좋아요 삽입, 1이면 좋아요 한 상황 -> 좋아요 삭제)
      boolean likeCheck = boardService.viewCheckLike(session, viewBoard);
      if (likeCheck == true) {
         model.addAttribute("likeCheck", likeCheck);
      } else if ( likeCheck == false) {
         model.addAttribute("likeCheck", likeCheck);
      }
      
      return "board/view";
   }      
   
   
   @GetMapping("/write")
   public void write() {}
   
   @PostMapping("/write")
   public String write( Board writeBoard, @RequestParam(required=false) List<MultipartFile> file, Model model, HttpSession session ){      
      logger.info("/board/write ❤️도착❤️");   
      logger.info("컨트롤러 보드 {}", writeBoard);
      logger.info("컨트롤러 파일 {}", file);
      
      int userNo = (Integer)session.getAttribute("userNo");
      writeBoard.setUserNo(userNo);
      
      boardService.write( writeBoard, file );
      model.addAttribute("writeBoard", writeBoard);
      
      return "redirect:./list";   //게시글 목록
   }
   
   @RequestMapping("/download")
   public String download(Files boardFile, Model model) {
      
      boardFile = boardService.getFile(boardFile);
      model.addAttribute("downFile", boardFile);
       
      return "down";
   }
   
   
   @GetMapping("/update")
   public String update( Board updateBoard, Model model ) {
      logger.info("update[GET] -> Controller 도착! {}");

      //잘못된 게시글 번호 처리
      if( updateBoard.getBoardNo() < 1 ) {
         return "redirect:/board/list";
      }
      
      //수정할 게시글의 상세보기
      updateBoard = boardService.view(updateBoard);
      model.addAttribute("updateBoard", updateBoard);
      
      //첨부파일 정보 모델값 전달
      List<Files> boardFile = boardService.getAttachFile(updateBoard);
      model.addAttribute("boardFile", boardFile);
         
      return "board/update";
   }
   
   @PostMapping("/update")
       public String update(Board updateBoard, List<MultipartFile> file, Model model) {
      logger.info("update[POST] -> Controller 도착! {}");

      updateBoard.getBoardNo();
      
      //게시글+첨부파일 수정
      boardService.update(updateBoard, file);
      model.addAttribute("updateBoard", updateBoard);
      
      //첨부파일 정보 모델값 전달
      List<Files> boardFile = boardService.getAttachFile(updateBoard);
      model.addAttribute("boardFile", boardFile);
      
       return "redirect:./view?boardNo=" + updateBoard.getBoardNo();
       }
      
   
   @RequestMapping("/delete")
   public String delete( Board board, Comments comments, HttpSession session ) {
      logger.info("/delete ❤️도착❤️");
      
      int userNo = (Integer)session.getAttribute("userNo");
      board.setUserNo(userNo);
      
      //댓글 조회 - (댓글이 있는 게시글 삭제를 위한 댓글 조회)
      int boardNo = board.getBoardNo();
      List<Comments> commentList = boardService.viewComment(comments);
      
      //댓글을 하나씩 삭제 (댓글이 있는 게시글 삭제를 위한 댓글 선삭제)
       for (Comments comment : commentList) {
           boardService.deleteComment(comment);
       }
      boardService.delete(board);
      
      return "redirect:./list";
   }
   
   
   @GetMapping("/like")
   @ResponseBody
   public void likeBoard(Likes like, Board board, Writer out, Model model, HttpSession session) {
      logger.info("/like ❤️도착❤️ : {}", like);
      
      //좋아요 상태 조회 (0이면 좋아요 안 한 상황 -> 좋아요 삽입, 1이면 좋아요 한 상황 -> 좋아요 삭제)
      boolean likeCheck = boardService.likeCheck(like);
      logger.info("좋아요 상태 확인 : {}", likeCheck);
      
      if (likeCheck == false) {
            
         //좋아요 이력 없음 -> 좋아요 삽입
         boardService.boardLike(like);
            
         //좋아요 수 조회
         int likeCount = boardService.getBoardLikeCount(like);
         logger.info("좋아요 수는 ? ZERO : {}", likeCount);
         
         try {
            out.write("{\"result\":true, \"likeCount\": \"" + likeCount + "\"}");
            logger.info("likeCheck - 뭘까? {}", likeCheck);
            
         } catch (IOException e) {
            e.printStackTrace();
            }
         } else if (likeCheck == true) {
            
            //좋아요 이력 있음 -> 좋아요 삭제
            boardService.boardDislike(like);
            
            //좋아요 수 조회
            int likeCount = boardService.getBoardLikeCount(like);
            logger.info("좋아요 수는 ? ONE : {}", likeCount);
            
            try {
               out.write("{\"result\":false, \"likeCount\": \"" + likeCount + "\"}");
               logger.info("likeCheck - 뭐지?? {}", likeCheck);
               
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
//      model.addAttribute("likeCount", likeCount);
      
//      logger.info("나와라!!!!!!!!!!!!!!! {}", likeCount);
      logger.info("나와라!!!!!!!!!!!!!!! {}", board.getLikeCount());
   }
   
   
   @GetMapping("/commentView")
   @ResponseBody
   public List<Comments> commentList(Board viewBoard, Comments comments, Model model) {
       
      Comments comment = new Comments();
      comment.setBoardNo(comment.getBoardNo());
      comment.setCommContent(comment.getCommContent());
      
      //댓글 조회
      int boardNo = viewBoard.getBoardNo();
      
      List<Comments> commentList = boardService.viewComment(comments);
      model.addAttribute("commentList", commentList);
      
      return commentList;
   }
   
   @PostMapping("/commentWrite")
   @ResponseBody
   public List<Comments> commentWrite(
         
         @RequestParam("boardNo") int boardNo, 
         @RequestParam("commContent") String commContent, Comments comments,
         Model model) {
      
       boardService.writeComment(comments);
       
       List<Comments> commentList = boardService.viewComment(comments);
       model.addAttribute("commentList", commentList);
       
       return commentList;
   }
   
   
   @PostMapping("/commentUpdate")
   @ResponseBody
   public List<Comments> commentUpdate(Comments comments, Model model) {
      
      logger.info("@@@@@@@@@@@@ commentUpdate()");
      
       logger.info("$$$$$$$$$$$$$$$$$$comments {}", comments);
       
      boardService.updateComment(comments);
      
      //수정된 댓글 리스트 조회
       List<Comments> commentList = boardService.viewComment(comments);
//       model.addAttribute("commentList", commentList);
       
//      return "redirect:./view?boardNo=" + board.getBoardNo();
       return commentList;
//      return null;
   }

   
   @RequestMapping("/commentDelete")
   @ResponseBody
   public List<Comments> commentDelete(Board viewBoard, Comments comments, Model model) {
      logger.info("/commentDelete ❤️도착❤️ ");
      
      //Comments 객체 생성 및 데이터 설정
       Comments comment = new Comments();
       comment.setBoardNo(viewBoard.getBoardNo());
       comment.setBoardNo(comments.getBoardNo());
       comment.setCommNo(comments.getCommNo());

       boardService.deleteComment(comment);

      //댓글 조회
      int boardNo = viewBoard.getBoardNo();
      
      //수정된 댓글 리스트 조회
       List<Comments> commentList = boardService.viewComment(comments);
       model.addAttribute("commentList", commentList);

       return commentList;
   }
   

}