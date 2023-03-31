package io.github.pnoker.center.auth.entity.builder;

import io.github.pnoker.center.auth.entity.bo.UserLoginBO;
import io.github.pnoker.center.auth.entity.vo.UserLoginVO;
import io.github.pnoker.common.model.UserLogin;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2023-04-01T00:38:38+0800",
    comments = "version: 1.5.3.Final, compiler: javac, environment: Java 1.8.0_321 (Oracle Corporation)"
)
@Component
public class UserLoginBuilderImpl implements UserLoginBuilder {

    @Override
    public UserLoginBO buildBOByVO(UserLoginVO entityVO) {
        if ( entityVO == null ) {
            return null;
        }

        UserLoginBO.UserLoginBOBuilder<?, ?> userLoginBO = UserLoginBO.builder();

        userLoginBO.id( entityVO.getId() );
        userLoginBO.remark( entityVO.getRemark() );
        userLoginBO.creatorId( entityVO.getCreatorId() );
        userLoginBO.creatorName( entityVO.getCreatorName() );
        userLoginBO.createTime( entityVO.getCreateTime() );
        userLoginBO.operatorId( entityVO.getOperatorId() );
        userLoginBO.operatorName( entityVO.getOperatorName() );
        userLoginBO.updateTime( entityVO.getUpdateTime() );
        userLoginBO.loginName( entityVO.getLoginName() );
        userLoginBO.userExtId( entityVO.getUserExtId() );
        userLoginBO.userPasswordId( entityVO.getUserPasswordId() );
        userLoginBO.enableFlag( entityVO.getEnableFlag() );

        return userLoginBO.build();
    }

    @Override
    public List<UserLoginBO> buildBOListByVOList(List<UserLoginVO> entityVOList) {
        if ( entityVOList == null ) {
            return null;
        }

        List<UserLoginBO> list = new ArrayList<UserLoginBO>( entityVOList.size() );
        for ( UserLoginVO userLoginVO : entityVOList ) {
            list.add( buildBOByVO(userLoginVO) );
        }

        return list;
    }

    @Override
    public UserLogin buildDOByBO(UserLoginBO entityBO) {
        if ( entityBO == null ) {
            return null;
        }

        UserLogin userLogin = new UserLogin();

        userLogin.setId( entityBO.getId() );
        userLogin.setRemark( entityBO.getRemark() );
        userLogin.setCreatorId( entityBO.getCreatorId() );
        userLogin.setCreatorName( entityBO.getCreatorName() );
        userLogin.setCreateTime( entityBO.getCreateTime() );
        userLogin.setOperatorId( entityBO.getOperatorId() );
        userLogin.setOperatorName( entityBO.getOperatorName() );
        userLogin.setLoginName( entityBO.getLoginName() );
        userLogin.setUserPasswordId( entityBO.getUserPasswordId() );
        userLogin.setEnableFlag( entityBO.getEnableFlag() );

        return userLogin;
    }

    @Override
    public List<UserLogin> buildDOListByBOList(List<UserLoginBO> entityBOList) {
        if ( entityBOList == null ) {
            return null;
        }

        List<UserLogin> list = new ArrayList<UserLogin>( entityBOList.size() );
        for ( UserLoginBO userLoginBO : entityBOList ) {
            list.add( buildDOByBO( userLoginBO ) );
        }

        return list;
    }

    @Override
    public UserLoginBO buildBOByDO(UserLogin entityDO) {
        if ( entityDO == null ) {
            return null;
        }

        UserLoginBO.UserLoginBOBuilder<?, ?> userLoginBO = UserLoginBO.builder();

        userLoginBO.id( entityDO.getId() );
        userLoginBO.remark( entityDO.getRemark() );
        userLoginBO.creatorId( entityDO.getCreatorId() );
        userLoginBO.creatorName( entityDO.getCreatorName() );
        userLoginBO.createTime( entityDO.getCreateTime() );
        userLoginBO.operatorId( entityDO.getOperatorId() );
        userLoginBO.operatorName( entityDO.getOperatorName() );
        userLoginBO.loginName( entityDO.getLoginName() );
        userLoginBO.userPasswordId( entityDO.getUserPasswordId() );
        userLoginBO.enableFlag( entityDO.getEnableFlag() );

        return userLoginBO.build();
    }

    @Override
    public List<UserLoginBO> buildBOByDO(List<UserLogin> entityDOList) {
        if ( entityDOList == null ) {
            return null;
        }

        List<UserLoginBO> list = new ArrayList<UserLoginBO>( entityDOList.size() );
        for ( UserLogin userLogin : entityDOList ) {
            list.add( buildBOByDO( userLogin ) );
        }

        return list;
    }

    @Override
    public UserLoginVO buildVOByBO(UserLoginBO entityBO) {
        if ( entityBO == null ) {
            return null;
        }

        UserLoginVO.UserVOBuilder<?, ?> userVO = UserLoginVO.builder();

        userVO.id( entityBO.getId() );
        userVO.remark( entityBO.getRemark() );
        userVO.creatorId( entityBO.getCreatorId() );
        userVO.creatorName( entityBO.getCreatorName() );
        userVO.createTime( entityBO.getCreateTime() );
        userVO.operatorId( entityBO.getOperatorId() );
        userVO.operatorName( entityBO.getOperatorName() );
        userVO.updateTime( entityBO.getUpdateTime() );
        userVO.loginName( entityBO.getLoginName() );
        userVO.userExtId( entityBO.getUserExtId() );
        userVO.userPasswordId( entityBO.getUserPasswordId() );
        userVO.enableFlag( entityBO.getEnableFlag() );

        return userVO.build();
    }

    @Override
    public List<UserLoginVO> buildVOListByBOList(List<UserLoginBO> entityBOList) {
        if ( entityBOList == null ) {
            return null;
        }

        List<UserLoginVO> list = new ArrayList<UserLoginVO>( entityBOList.size() );
        for ( UserLoginBO userLoginBO : entityBOList ) {
            list.add( buildVOByBO( userLoginBO ) );
        }

        return list;
    }
}
