package octo.cm.dto;

import cmn.anotation.ClassDeclare;
import gpf.adur.data.Password;
import gpf.adur.user.User;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Comment("用户Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-12-30", updateTime = "2025-12-30"
)
public class UserDto implements Serializable {
    private String code;
    private String userName;
    private String alias;
    private String gender;
    private String profilePhoto;
    private String passwd;
    private String status;
    private String phone;
    private String email;
    private String sourceRoleCode;


    // ========================= 支撑方法 =========================

    // 将UserDto转换为User系统对象
    public User toUser(String userModelID) {
        User user = new User(userModelID);
        user.setCode(this.code);
        user.setUserName(this.userName);
        user.setFullName(this.alias);
        user.setGender(this.gender);
        if (this.passwd != null) {
            user.setPassword(new Password().setValue(this.passwd));
        }
        user.setPhone(this.phone);
        user.setEmail(this.email);
        return user;
    }

    // 将User系统对象转换为UserDto
    public static UserDto fromUser(User user) {
        if (user == null) return null;
        try {
            UserDto dto = new UserDto();
            dto.setCode(user.getCode());
            dto.setUserName(user.getUserName());
            dto.setAlias(user.getFullName());
            dto.setGender(user.getGender());
            dto.setPhone(user.getPhone());
            dto.setEmail(user.getEmail());
            dto.setStatus(user.getStatus());
            dto.setPasswd(user.getPassword().getValue());
            return dto;
        } catch (Exception e) {
            return null;
        }
    }

    // 批量转换User列表为UserDto列表
    public static List<UserDto> fromUserList(List<User> userList) {
        if (userList == null) return Collections.emptyList();
        return userList.stream().map(UserDto::fromUser).collect(Collectors.toList());
    }


    // ========================= getter/setter =========================

    public String getCode() {
        return code;
    }

    public UserDto setCode(String code) {
        this.code = code;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public UserDto setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public UserDto setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public String getGender() {
        return gender;
    }

    public UserDto setGender(String gender) {
        this.gender = gender;
        return this;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public UserDto setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
        return this;
    }

    public String getPasswd() {
        return passwd;
    }

    public UserDto setPasswd(String passwd) {
        this.passwd = passwd;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public UserDto setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public UserDto setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getSourceRoleCode() {
        return sourceRoleCode;
    }

    public UserDto setSourceRoleCode(String sourceRoleCode) {
        this.sourceRoleCode = sourceRoleCode;
        return this;
    }


}
