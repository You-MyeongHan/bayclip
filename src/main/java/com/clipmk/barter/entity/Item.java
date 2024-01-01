package com.clipmk.barter.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.clipmk.barter.dto.ItemResDto;
import com.clipmk.region.entity.Region;
import com.clipmk.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.annotation.Nullable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamicInsert
@DynamicUpdate
@Entity
@Table(name="item")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Item {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String title;
	private String content;
	private String category;
	@ElementCollection
    private List<String> img_src;
	@ColumnDefault("0")
	@Column(name="view_cnt")
	private Integer viewCnt;
	@CreationTimestamp
	private LocalDateTime  wr_date;
	private LocalDateTime  re_date;
	private LocalDateTime  del_date;
	@ManyToOne(fetch = FetchType.EAGER, optional = true)
	@JoinColumn(name = "region_code")
	private Region region;
	private Integer status;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
	@JsonIgnore
    private User user;
	
	@OneToMany(mappedBy = "toItem")
	private List<Deal> suggestedDeals;
	
	@Builder.Default
	@ElementCollection
	@CollectionTable(
		name="item_dib",
		joinColumns=@JoinColumn(name="item_id")
	)
	@Column(name="user_id")
	private Set<Integer> dib=new HashSet<>();
	
	public ItemResDto toDto() {
		ItemResDto itemDto = ItemResDto.builder()
				.id(this.id)
				.title(this.title)
				.content(this.content)
				.img_src(this.img_src)
				.category(this.category)
				.viewCnt(this.viewCnt)
				.wr_date(this.wr_date)
				.re_date(this.re_date)
				.region(this.region)
				.status(this.status)
				.del_date(this.del_date)
				.dib_cnt(this.getDib().size())
				.user_id(this.user.getId())
				.user_nick(this.user.getNick())
				.build();
		
		return itemDto;
	}
}
