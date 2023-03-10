package com.eyelevel.project.category.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eyelevel.project.category.dto.FilesDTO;
import com.eyelevel.project.category.dto.PwdDTO;
import com.eyelevel.project.category.dto.TeacherAreaAndTeachingAreaDTO;
import com.eyelevel.project.category.dto.TeacherImpl;
import com.eyelevel.project.category.dto.TeacherProfileAndTeacherAreaDTO;
import com.eyelevel.project.category.entity.Files;
import com.eyelevel.project.category.entity.TeacherAreaAndTeachingArea;
import com.eyelevel.project.category.entity.TeacherProfileAndTeacherArea;
import com.eyelevel.project.category.repository.FilesRepository;
import com.eyelevel.project.category.repository.TeacherAreaAndTeacherProfileRepository;
import com.eyelevel.project.category.repository.TeacherAreaAndTeachingAreaRepository;
import com.eyelevel.project.category.repository.TeacherProfileAndTeacherAreaRepository;
import com.eyelevel.project.category.repository.TeacherProfileRepository;
import com.eyelevel.project.common.paging.SelectCriteria;

import net.coobird.thumbnailator.Thumbnails;

@Service
public class TeacherProfileService {

	private final TeacherProfileAndTeacherAreaRepository teacherProfileAndTeacherAreaRepository;
	private final TeacherAreaAndTeacherProfileRepository teacherAreaAndTeacherProfileRepository;
	private final FilesRepository filesRepository;
	private final TeacherProfileRepository teacherProfileRepository;
	private final TeacherAreaAndTeachingAreaRepository teacherAreaAndTeachingAreaRepository;
	private final ModelMapper modelMapper;
	private final BCryptPasswordEncoder bCryptPwdEncoder;
	
	@Autowired
	public TeacherProfileService(TeacherProfileAndTeacherAreaRepository teacherProfileAndTeacherAreaRepository,
			TeacherAreaAndTeacherProfileRepository teacherAreaAndTeacherProfileRepository,
			FilesRepository filesRepository, TeacherProfileRepository teacherProfileRepository,
			TeacherAreaAndTeachingAreaRepository teacherAreaAndTeachingAreaRepository, ModelMapper modelMapper,
			BCryptPasswordEncoder bCryptPwdEncoder) {
		this.teacherProfileAndTeacherAreaRepository = teacherProfileAndTeacherAreaRepository;
		this.teacherAreaAndTeacherProfileRepository = teacherAreaAndTeacherProfileRepository;
		this.filesRepository = filesRepository;
		this.teacherProfileRepository = teacherProfileRepository;
		this.teacherAreaAndTeachingAreaRepository = teacherAreaAndTeachingAreaRepository;
		this.modelMapper = modelMapper;
		this.bCryptPwdEncoder = bCryptPwdEncoder;
	}
	
	/* ????????? ?????? */
	public int selectTotalCount(String searchCondition, String searchValue) {
		
		int count = 0;
		if(searchValue != null) {
			if("teacherNo".equals(searchCondition)) {
				count = teacherProfileAndTeacherAreaRepository.countByTeacherNoContaining(searchValue);
			}
			if("teacherName".equals(searchCondition)) {
				count = teacherProfileAndTeacherAreaRepository.countByTeacherNameContaining(searchValue);
			}
		} else {
			count = (int)teacherProfileAndTeacherAreaRepository.count();
		}

		return count;
	}

	/* ????????? ?????? (?????? ??????) */
	public List<TeacherProfileAndTeacherAreaDTO> searchTeacherProfileAndTeacherArea(SelectCriteria selectCriteria) {
		
		int index = selectCriteria.getPageNo() - 1;
		int count = selectCriteria.getLimit();
		String searchValue = selectCriteria.getSearchValue();

		Pageable paging = PageRequest.of(index, count, Sort.by("teacherNo"));

		List<TeacherProfileAndTeacherArea> profileList = new ArrayList<TeacherProfileAndTeacherArea>();
		if(searchValue != null) {

			/* [?????? ??????] */
			if("teacherNo".equals(selectCriteria.getSearchCondition())) {
				profileList = teacherProfileAndTeacherAreaRepository.findByTeacherNoContaining(selectCriteria.getSearchValue(), paging);
			}
			/* [?????? ??????] */
			if("teacherName".equals(selectCriteria.getSearchCondition())) {
				profileList = teacherProfileAndTeacherAreaRepository.findByTeacherNameContaining(selectCriteria.getSearchValue(), paging);
			}
		} else {
			profileList = teacherProfileAndTeacherAreaRepository.findAll(paging).toList();
		}

		return profileList.stream().map(profile -> modelMapper.map(profile, TeacherProfileAndTeacherAreaDTO.class)).collect(Collectors.toList());
	}
	
	/* [????????? : ?????????, ??????] ?????? ???????????? ?????? ????????? */
	public TeacherProfileAndTeacherAreaDTO findTeacherProfileAndTeacherAreaByTeacherNo(String teacherNo) {
		
		TeacherProfileAndTeacherArea detailProfile = teacherProfileAndTeacherAreaRepository.findById(teacherNo).get();
		
		return modelMapper.map(detailProfile, TeacherProfileAndTeacherAreaDTO.class);
	}

	/* [????????? : ?????????, ??????] ?????? ???????????? ?????? ????????? (????????????) */
	public List<TeacherAreaAndTeachingAreaDTO> findTeacherAreaListByAreaNo(String teacherNo) {
		
		List<TeacherAreaAndTeachingArea> teacherAreaList = teacherAreaAndTeachingAreaRepository.findByTeacherNoContaining(teacherNo);
		System.out.println("teacherAreaList : " + teacherAreaList);
		
		return teacherAreaList.stream().map(area -> modelMapper.map(area, TeacherAreaAndTeachingAreaDTO.class)).collect(Collectors.toList());
	}
	
	/* [?????? : ?????????, ??????] ?????? / ?????? ???????????? ??? ?????? ?????? ?????? */
	@Transactional
	public void modifyProfileAndArea(TeacherProfileAndTeacherAreaDTO newProfile, MultipartFile mfile, FilesDTO newFile) {
		
		/* ?????? ?????? ??? ?????? */
		// ?????? ?????? ??? ?????? [??????????????? ?????? ??? ?????? ?????? ??????!!!!!!!!!!!!!!!!!!!!!!!!!??????????????? ???????????? ???????????? ???????????????]
//		String filepath = "C:\\00_personalPJ\\eyeLevelProject\\src\\main\\resources\\static\\image\\";
		String filepath = "C:\\00_eyelevel\\eyeLevelProject\\src\\main\\resources\\static\\image\\";
		String thumbnailPath = "\\image\\";
		File directory = new File(filepath);
		if(!directory.exists()) {
			directory.mkdirs();
		}
		
		/* ???????????? ????????? ?????? ???????????? ????????? ?????? ?????? (?????? ????????? ?????? ?????? ?????? ?????? -> ???????????? ?????? ????????? ??????) */
		/* files ???????????? ???????????? ???????????? ??????????????? ??????????????? ???????????? + ?????? ???????????? + ?????? ?????? ?????? 3?????? ?????????????????? */
		// ?????? ?????? ??? ??????
		String originFileName = mfile.getOriginalFilename();
		System.out.println("????????????????????? originFileName : " + originFileName);
		if(!originFileName.isEmpty()) {
			String extension = originFileName.substring(originFileName.lastIndexOf("."));
			String saveName = UUID.randomUUID().toString().replace("-", "") + extension;
			// ?????? ??????
			try {
				mfile.transferTo(new File(filepath + saveName));
				Thumbnails.of(filepath + saveName).size(300, 400).toFile(filepath + "thumbnail_" + saveName);
			} catch (IllegalStateException | IOException e) {
				e.printStackTrace();
				new File(filepath + "/" + saveName).delete();
			}
			
			/* ?????? 1?????? ????????? ????????? ???????????? ?????? ????????? */
			if(filesRepository.findByTeacherNo(newProfile.getTeacherNo()) == null) {
				/* ?????? ?????? ???????????? ?????? ????????? ?????? ?????? (?????? ??????) */
				newFile.setTeacherNo(newProfile.getTeacherNo());
				newFile.setFileType(extension);
				newFile.setFileOriginName(originFileName);
				newFile.setFileSaveName(saveName);
				newFile.setFilePath(filepath);
				newFile.setThumbnailPath(thumbnailPath + "thumbnail_" + saveName);
				filesRepository.save(modelMapper.map(newFile, Files.class));
				
				TeacherProfileAndTeacherArea profile = teacherProfileAndTeacherAreaRepository.findById(newProfile.getTeacherNo()).get();
				
				profile.setTeacherPicture(newFile.getThumbnailPath());
				profile.setTeacherAddress(newProfile.getTeacherAddress());
				profile.setTeacherPhone(newProfile.getTeacherPhone());
				profile.setTeacherEmail(newProfile.getTeacherEmail());
				profile.setTeacherLevel(newProfile.getTeacherLevel());
				profile.setTeacherPr(newProfile.getTeacherPr());
			} else {
				/* ?????? ?????? ???????????? ?????? ????????? ?????? ?????? (??????) */
				Files file = filesRepository.findByTeacherNo(newProfile.getTeacherNo());
				file.setFileType(extension);
				file.setFileOriginName(originFileName);
				file.setFileSaveName(saveName);
				file.setFilePath(filepath);
				file.setThumbnailPath(thumbnailPath + "thumbnail_" + saveName);
				
				TeacherProfileAndTeacherArea profile = teacherProfileAndTeacherAreaRepository.findById(newProfile.getTeacherNo()).get();
				
				profile.setTeacherPicture(file.getThumbnailPath());
				profile.setTeacherAddress(newProfile.getTeacherAddress());
				profile.setTeacherPhone(newProfile.getTeacherPhone());
				profile.setTeacherEmail(newProfile.getTeacherEmail());
				profile.setTeacherLevel(newProfile.getTeacherLevel());
				profile.setTeacherPr(newProfile.getTeacherPr());
			}
			
		/* ??????????????? ????????? ??? ?????? ???????????? ????????? ?????? ?????? */
		} else {
			TeacherProfileAndTeacherArea profile = teacherProfileAndTeacherAreaRepository.findById(newProfile.getTeacherNo()).get();
			
			// ?????? ????????? ??????????????? ?????? ?????? ????????? ????????? ????????? ??????
			profile.setTeacherAddress(newProfile.getTeacherAddress());
			profile.setTeacherPhone(newProfile.getTeacherPhone());
			profile.setTeacherEmail(newProfile.getTeacherEmail());
			profile.setTeacherLevel(newProfile.getTeacherLevel());
			profile.setTeacherPr(newProfile.getTeacherPr());
			// ??? ?????? ?????? ?????? ??????
		}
	}

	/* [?????? : ?????????] ?????? / ?????? ?????? ?????? ?????? */
	@Transactional
	public void modifyProfile(TeacherProfileAndTeacherAreaDTO newProfile) {
		
		TeacherProfileAndTeacherArea profile = teacherProfileAndTeacherAreaRepository.findById(newProfile.getTeacherNo()).get();
		profile.setTeacherEnt('Y');
	}
	
	/* [?????? : ?????????] ?????? / ?????? ???????????? ??? ?????? ?????? ?????? */
	@Transactional
	public void deleteProfileAndArea(TeacherProfileAndTeacherAreaDTO newProfile) {
		
		teacherProfileAndTeacherAreaRepository.deleteById(newProfile.getTeacherNo());
	}

	/* [?????? : ?????????, ??????] ?????? / ?????? ???????????? */
	@Transactional
	public int modifyPwd(TeacherImpl teacher, PwdDTO pwd) {
		
		TeacherProfileAndTeacherArea profile = teacherProfileAndTeacherAreaRepository.findById(teacher.getTeacherNo()).get();
		String teacherPw = profile.getTeacherPw();
		String originPw = pwd.getOriginPw();
		String newPw = pwd.getNewPw();
		String checkPw = bCryptPwdEncoder.encode(pwd.getCheckPw());
		
		if(bCryptPwdEncoder.matches(originPw, teacherPw) && bCryptPwdEncoder.matches(newPw, checkPw)) {
			/* ?????? ??????????????? ????????????, ?????? ??????????????? ?????? ??????????????? ???????????? ?????? */
			profile.setTeacherPw(checkPw);
			return 1;
			
		} else if(bCryptPwdEncoder.matches(originPw, teacherPw) && !bCryptPwdEncoder.matches(newPw, checkPw)) {
			/* ?????? ??????????????? ???????????????, ?????? ??????????????? ?????? ??????????????? ???????????? ?????? ?????? */
			return 2;
			
		} else {
			/* ?????? ??????????????? ???????????? ?????? ?????? */
			return 3;
		}
	}
	
	/* ?????? ?????? */
	public void registNewTeacher(TeacherProfileAndTeacherAreaDTO newTeacher) {
		teacherProfileAndTeacherAreaRepository.save(modelMapper.map(newTeacher, TeacherProfileAndTeacherArea.class));
		
	}
}