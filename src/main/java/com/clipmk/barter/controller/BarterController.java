package com.clipmk.barter.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clipmk.barter.dto.DealAcceptReqDto;
import com.clipmk.barter.dto.EditItemReqDto;
import com.clipmk.barter.dto.ItemReqDto;
import com.clipmk.barter.dto.ItemResDto;
import com.clipmk.barter.dto.ItemsResDto;
import com.clipmk.barter.entity.Deal;
import com.clipmk.barter.service.BarterService;
import com.clipmk.barter.specification.ItemSpecifications;
import com.clipmk.chat.entity.ChatRoom;
import com.clipmk.chat.service.ChatService;
import com.clipmk.user.entity.User;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/barter")
@RequiredArgsConstructor
public class BarterController {
	private final BarterService barterService;
	private final ChatService chatService;
	
	//아이템 등록
	@PostMapping("/item")
	public ResponseEntity<Void> register(
			@RequestBody ItemReqDto request,
			@AuthenticationPrincipal User user
	){
		
		barterService.register(request, user.getId());
		
		return ResponseEntity.ok().build();
	}
	
	//아이템 조회
	@GetMapping("/item/{item-id}")
	public ResponseEntity<ItemResDto> getItemById(
			@PathVariable("item-id") Long itemId,
			HttpSession session,
			@AuthenticationPrincipal User user){
		
		ItemResDto itemDto=barterService.getItemById(itemId, user, session);
		
		return ResponseEntity.ok(itemDto);
	}
	
	//아이템 수정
	@PatchMapping("/item/{item-id}")
	public ResponseEntity<ItemResDto> editItem(
			@PathVariable("item-id") Long itemId,
			@RequestBody EditItemReqDto request,
			@AuthenticationPrincipal User user
	){
		ItemResDto itemDto=barterService.edit(itemId, request, user);
		
		if(itemDto != null) {
			return ResponseEntity.ok(itemDto);
		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	//아이템 삭제
	@DeleteMapping("/item/{item-id}")
	public ResponseEntity<Void> deleteItem(
			@PathVariable("item-id") Long itemId,
			@AuthenticationPrincipal User user
	){	
		
		barterService.delete(itemId, user);
		return ResponseEntity.noContent().build();
	}
	
	//아이템 페이징
	@GetMapping("/items")
	public ResponseEntity<Page<ItemsResDto>> items(
			@PageableDefault(page=0, size = 20, sort = {"id"}, direction = Sort.Direction.DESC) Pageable pageable,
			@RequestParam(value="category", defaultValue = "") String category,
			@RequestParam(value = "searchTerm", defaultValue  = "") String searchTerm,
			@RequestParam(value = "regionCode", defaultValue  = "") String regionCode
			){
		Page<ItemsResDto> items = barterService.findAll(ItemSpecifications.filterItems(category, searchTerm, regionCode), pageable)
	            .map(ItemsResDto::from);
		
		return ResponseEntity.ok(items);
	}
	
	//최근 6개의 아이템
	@GetMapping("/items6")
	public ResponseEntity<Page<ItemsResDto>> items6(){
		Page<ItemsResDto> items = barterService.find6()
	            .map(ItemsResDto::from);
		return ResponseEntity.ok(items);
	}
	
	//아이템 찜
	@PatchMapping("/item/{item-id}/dib")
	public ResponseEntity<Void> dib(
			@PathVariable(value="item-id") Long itemId,
			@AuthenticationPrincipal User user){
		
		barterService.dib(itemId, user);
		
		return ResponseEntity.ok().build();
	}
	
	
	//거래 제안
	@PostMapping("/item/{to-item-id}/suggest/{from-item-id}")
	public ResponseEntity<Void> suggestDeal(
			@PathVariable(value="to-item-id") Long toItemId,
			@PathVariable(value="from-item-id") Long fromItemId,
			@AuthenticationPrincipal User user
	){
		
		barterService.suggestDeal(fromItemId, toItemId, user);
		
		return ResponseEntity.ok().build();
	}

	//아이템 상태 변경
	@PutMapping("/item/{item-id}/restate")
	public ResponseEntity<Void> restate(
			@PathVariable(value="item-id") Long itemId,
			@AuthenticationPrincipal User user
	){	
		
		barterService.restate(itemId, user);
		
		return ResponseEntity.ok().build(); 
		
	}
	//거래 수락
	@PostMapping("/accept")
	public ResponseEntity<Long> acceptDeal(
			@RequestBody DealAcceptReqDto request,
			@AuthenticationPrincipal User user
	){
		Deal deal = barterService.getDealById(request.getDealId());
		
		if(deal != null) {
			if(user.getId() == deal.getToItem().getUser().getId()) {
//				long chatRoomId = chatService.createChatRoom(deal);
				ChatRoom chatRoom = chatService.createChatRoom(request.getUserId1(),request.getUserId2());
				barterService.dealOn(deal);
				return ResponseEntity.ok(chatRoom.getId());
			}
			return ResponseEntity.badRequest().build();
			
		}
		
		return ResponseEntity.badRequest().build();
		
	}
	
	//거래 거절
	@PostMapping("/reject")
	public ResponseEntity<Void> rejectDeal(
			@AuthenticationPrincipal User user
	){
		return ResponseEntity.ok().build();
	}
	
	//거래 완료
	@PostMapping("/complete")
	public ResponseEntity<Void> completeDeal(
			
			@AuthenticationPrincipal User user
	){
		return ResponseEntity.ok().build();
	}
	
}
