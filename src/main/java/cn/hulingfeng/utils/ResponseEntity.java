package cn.hulingfeng.utils;

import java.io.Serializable;

/**
 * @author hlf
 * @title: ResponseEntity
 * @projectName es
 * @description: TODO
 * @date 2020/2/9 16:10
 */
public class ResponseEntity<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private String msg;
    private Integer code;
    private T object;

    public ResponseEntity() {
    }

    public static ResponseEntity getInstance(String msg, Integer code) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setMsg(msg);
        responseEntity.setCode(code);
        return responseEntity;
    }

    public static ResponseEntity getInstance(String msg, Integer code,Object object) {
        ResponseEntity responseEntity = new ResponseEntity();
        responseEntity.setMsg(msg);
        responseEntity.setCode(code);
        responseEntity.setObject(object);
        return responseEntity;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public String getMsg() {
        return msg;
    }

    public Integer getCode() {
        return code;
    }

    public T getObject() {
        return object;
    }

    @Override
    public String toString() {
        return "ResponseEntity{" +
                "msg='" + msg + '\'' +
                ", code=" + code +
                ", object=" + object +
                '}';
    }

}
