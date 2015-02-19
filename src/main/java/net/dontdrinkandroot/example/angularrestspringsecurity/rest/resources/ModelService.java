package net.dontdrinkandroot.example.angularrestspringsecurity.rest.resources;

import net.dontdrinkandroot.example.angularrestspringsecurity.dao.newsentry.NewsEntryDao;
import net.dontdrinkandroot.example.angularrestspringsecurity.dao.user.UserDao;
import net.dontdrinkandroot.example.angularrestspringsecurity.entity.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

public class ModelService {
	private static ModelService instance;
	private PasswordEncoder passwordEncoder;

	private UserDao userDao;
	private NewsEntryDao newsEntryDao;
	public ModelService() {
		ModelService.instance=this;
	}
	public static ModelService getInstance(){
		if(instance==null)instance=new 	ModelService();
		return instance;
	}

	public ModelService(UserDao userDao,NewsEntryDao newsEntryDao, PasswordEncoder passwordEncoder) {
		ModelService.instance=this;
		this.setUserDao(userDao);
		this.setNewsEntryDao(newsEntryDao);
		this.setPasswordEncoder(passwordEncoder);
	}

	public UserDao getUserDao() {
		return userDao;
	}

	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}

	public PasswordEncoder getPasswordEncoder() {
		return passwordEncoder;
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}
	
	public NewsEntryDao getNewsEntryDao() {
		return newsEntryDao;
	}
	public void setNewsEntryDao(NewsEntryDao newsEntryDao) {
		this.newsEntryDao = newsEntryDao;
	}
}
