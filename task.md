Написать приложение на джаве, в которое можно по загружать файлы и по API получать список загруженных файлов. 
Фронтенд не нужен, только API.
Запрос на получение списка файлов должен быть параметризуемым: 
можно получить список файлов определенного типа, больше/меньше/равно определенного размера.

Архитектура: загрузка файлов должна обрабатываться одним сервисом, их анализ (тип, размер) -- в другом. 
Результаты анализа должны храниться в БД postgres, сами файлы -- в s3 (minio). 
Для работы с postgres использовать hibernate.

Для простоты можно использовать Spring Cloud (например, для сервис дискавери взять eureka).