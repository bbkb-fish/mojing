package com.mojing.novel.common;

import com.mojing.novel.completion.AiProviderException;
import com.mojing.novel.qdrant.VectorStoreException;
import com.mojing.novel.writing.WritingConflictException;
import com.mojing.novel.writing.WritingNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        return new ApiError(fieldError == null ? "请求参数不正确" : fieldError.getDefaultMessage());
    }

    @ExceptionHandler(AiProviderException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiError handleAiProvider(AiProviderException exception) {
        return new ApiError(exception.getMessage());
    }

    @ExceptionHandler(VectorStoreException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiError handleVectorStore(VectorStoreException exception) {
        return new ApiError(exception.getMessage());
    }

    @ExceptionHandler(WritingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(WritingNotFoundException exception) {
        return new ApiError(exception.getMessage());
    }

    @ExceptionHandler(WritingConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(WritingConflictException exception) {
        return new ApiError(exception.getMessage());
    }
}
