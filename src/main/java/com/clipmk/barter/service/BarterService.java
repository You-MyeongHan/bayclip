package com.clipmk.barter.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clipmk.barter.dto.DealsResDto;
import com.clipmk.barter.dto.EditItemReqDto;
import com.clipmk.barter.dto.ItemReqDto;
import com.clipmk.barter.dto.ItemResDto;
import com.clipmk.barter.entity.Deal;
import com.clipmk.barter.entity.Item;
import com.clipmk.barter.repository.BarterRepository;
import com.clipmk.barter.repository.DealRepository;
import com.clipmk.region.entity.Region;
import com.clipmk.region.repository.RegionRepository;
import com.clipmk.user.entity.User;
import com.clipmk.user.repository.UserRepository;
import com.global.error.errorCode.BarterErrorCode;
import com.global.error.exception.RestApiException;
import com.infra.meta.dto.ItemIdDto;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BarterService {
	private final BarterRepository barterRepository;
	private final DealRepository dealRepository;
	private final UserRepository userRepository;
	private final RegionRepository regionRepository;
	
	// 아이템 조회
	@Transactional
	public ItemResDto getItemById(Long itemId, User user, HttpSession session) {
		Item item=barterRepository.findItemById(itemId).orElseThrow(
				()-> new RestApiException(BarterErrorCode.ITEM_NOT_FOUND));
		
//		Set<Long> viewedItemIds = (Set<Long>) session.getAttribute("viewedItemIds");
//		if (viewedItemIds == null) {
//			viewedItemIds = new HashSet<>();
//        }
//		if (!viewedItemIds.contains(itemId)) {
//        	item.setViewCnt(item.getViewCnt()+1);
//            barterRepository.save(item);
//            viewedItemIds.add(itemId);
//        }
//		session.setAttribute("viewedPostIds", viewedItemIds);
		ItemResDto itemResDto = item.toDto();
		
		//제시된 거래 보여주기 && 찜 상태 보내주기
		if(user!=null ) {
			var dibs = item.getDib().contains(user.getId());
			if(dibs)
				itemResDto.setDib_state(true);
			else if(!dibs)
				itemResDto.setDib_state(false);
		}
		Pageable pageable = PageRequest.of(0, 20);
		Page<DealsResDto> dealsResDto=dealRepository.findByToItemId(item.getId(), pageable).map(DealsResDto::from);
		itemResDto.setDeals(dealsResDto);
		
		

		return itemResDto;
		
	}
	
	//아이템 등록
	@Transactional
	public Boolean register(ItemReqDto request, Integer userId) {
		
		User user=userRepository.findById(userId).orElse(null);
		Region region = regionRepository.findByCode(request.getRegionCode())
                .orElseThrow(null);
		
		//썸네일 순서 반영
		
		Item item = Item.builder()
				.title(request.getTitle())
				.category(request.getCategory())
				.content(request.getContent())
				.img_src(request.getImgSrc())
				.region(region)
				.status(1)
				.user(user)
				.build();
		
		barterRepository.save(item);
		
		return true;
	}
	
	//아이템 수정
	@Transactional
	public ItemResDto edit(Long itemId, EditItemReqDto request, User user) {
		
		Item item=barterRepository.findItemById(itemId).orElseThrow(
				()-> new RestApiException(BarterErrorCode.ITEM_NOT_FOUND));
		
		if (!item.getUser().equals(user)) {
	        throw new RestApiException(BarterErrorCode.ITEM_ACCESS_DENIED);
	    }
		
		item.setTitle(request.getTitle());
		item.setContent(request.getContent());
		item.setCategory(request.getCategory());
		item.setImg_src(request.getImgSrc());
		item.setRe_date(LocalDateTime.now());
		Item newItem = barterRepository.save(item);
		
		return newItem.toDto();

	}
	
	//아이템 삭제
	@Transactional
	public void delete(Long itemId, User user) {
		
		Item item=barterRepository.findItemById(itemId).orElseThrow(
				()-> new RestApiException(BarterErrorCode.ITEM_NOT_FOUND));
		
		if (!item.getUser().equals(user)) {
	        throw new RestApiException(BarterErrorCode.ITEM_ACCESS_DENIED);
	    }
		
		barterRepository.delete(item);
	}
	
	//아이템 페이징
	public Page<Item> findAll(Specification<Item> spec, Pageable pageable){
        return barterRepository.findAll(spec, pageable);
	}
	
	public Page<Item> find6(){
		Pageable spec = PageRequest.of(0, 6, Sort.by(Sort.Order.desc("id")));
        return barterRepository.findAll(spec);
	}
	
	//아이템 검색
	public Page<Item> findByTitleContaining(Pageable pageable, String searchTerm){
		return barterRepository.findByTitleContaining(pageable, searchTerm);
	}
	
	//아이템 찜
	@Transactional
	public void dib(Long itemId, User user) {
		Item item = barterRepository.findById(itemId).orElseThrow(
				()-> new RestApiException(BarterErrorCode.ITEM_NOT_FOUND));
		
		Set<Integer> dib = item.getDib();
		
		if(dib.contains(user.getId())) {
			dib.remove(user.getId());
		}else {
			dib.add(user.getId());
		}
		barterRepository.save(item);
	}
	
	//아이템 상태 변경
	public void restate(Long itemId, User user) {
		Item item = barterRepository.findById(itemId).orElseThrow(
				() -> new RestApiException(BarterErrorCode.ITEM_NOT_FOUND));
		
		if(item.getUser().equals(user)) {
			;
		}
		
	}
	
	//거래 제안
	public void suggestDeal(Long fromItemId, Long toItemId, User fromUser) {
		Item fromItem = barterRepository.findById(fromItemId).orElseThrow(() -> new EntityNotFoundException("Item not found"));
        Item toItem = barterRepository.findById(toItemId).orElseThrow(() -> new EntityNotFoundException("Item not found"));
        User toUser = toItem.getUser();
        
        Deal deal = Deal.builder()
                .fromUser(fromUser) // 거래 제안자
                .toUser(toUser) // 거래 제안을 받은 사용자
                .fromItem(fromItem) // 거래 제안을 보낸 아이템
                .toItem(toItem) // 거래 제안을 받은 아이템
                .status(0) // 상태 초기화 (0은 거래가 제안됨을 나타냄)
                .wr_date(LocalDateTime.now()) // 거래 생성일자
                .build();

        // 거래를 데이터베이스에 저장
        dealRepository.save(deal);
        
	}
	
	public Page<Item> findByUserId(int userId, Pageable pageable){
		Page<Item> Items = barterRepository.findByUserId(userId, pageable);
		return Items;
	}
	
	public Page<Item> findDibByUserId(int userId, Pageable pageable){
		Page<Item> Items = barterRepository.findDibItemsByUserId(userId, pageable);
		return Items;
	}
	
	//거래 아이디로 거래 조회
	public Deal getDealById(long dealId) {
		return dealRepository.findById(dealId).orElse(null);
	}
	
	public void dealOn(Deal deal) {
		deal.setStatus(1);
		dealRepository.save(deal);
	}
	
	public List<ItemIdDto> getItemsIds(){
		List<ItemIdDto> itemsIds= barterRepository.findAllItemIds();
		return itemsIds;
	}
	
}