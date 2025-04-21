//对应springboot传来的分页数据的模型

// 通用的分页响应接口，使用泛型
export interface Page<T> {
    content: T[];
    page: PageProps;
}

// 分页属性
export interface PageProps {
    size: number;
    totalElements: number;
    totalPages: number;
    number: number;
}
  
