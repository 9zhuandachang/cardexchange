package com.example.cardexchange;

import org.springframework.http.HttpStatus;

public class ResponseMessage<T> {
private Integer code;
private String message;
private T data;

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public ResponseMessage(Integer code, String message, T data)
{
    this.code=code ;
    this.message=message;
    this.data=data;

}
public static <T> ResponseMessage<T> success(T data)
{


    return new ResponseMessage<>(HttpStatus.OK.value(),"success",data);
}

    public static <T> ResponseMessage<T> badRequest(T data) {
        return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(), "error", data);
    }
    public static <T> ResponseMessage<T> unauthorized(T data) {
        return new ResponseMessage<>(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", data);
    }

    public static <T> ResponseMessage<T> badRequestString(String message) {
        return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(), message, null);
    }


}
