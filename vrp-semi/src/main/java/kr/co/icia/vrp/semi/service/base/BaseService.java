package kr.co.icia.vrp.semi.service.base;
import java.io.Serializable;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import kr.co.icia.vrp.semi.dao.base.BaseDao;
import kr.co.icia.vrp.semi.dao.base.Page;
import kr.co.icia.vrp.semi.parameter.SearchParameter;
public class BaseService<T, ID extends Serializable, Dao extends BaseDao<T, ID>> {
  @Autowired
  protected Dao dao;
  @Transactional(readOnly = true)
  public List<T> getList(SearchParameter searchParameter) { // 여기서 나와있는 List<t>는 노드서비스로 가면 t=Node와 같음.
    return dao.selectList(searchParameter);
  }
  @Transactional(readOnly = true)
  public long getListCount(SearchParameter searchParameter) {
    return dao.selectListCount(searchParameter);
  }
  @Transactional(readOnly = true)
  public Page<T> getPage(SearchParameter searchParameter) {
     List<T> contents = dao.selectList(searchParameter);
     long totalCount = dao.selectListCount(searchParameter);
     return new Page<>(contents, searchParameter.getPageable(), totalCount);
  }
  @Transactional(readOnly = true)
  public T getOne(ID id) {
    return dao.selectOne(id);
  }
  @Transactional(readOnly = true)
  public T getOneByParam(SearchParameter searchParameter) {
    return dao.selectOneByParam(searchParameter);
  }
  @Transactional
  public int add(T t) {
    return dao.insert(t);
  }
  @Transactional
  public int mod(T t) {
    return dao.update(t);
  }
  @Transactional
  public int del(ID id) {
    return dao.delete(id);
  }
}
