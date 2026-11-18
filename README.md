# my-blog-back-app
Бэкенд-приложение для персонального блога с поддержкой постов, тегов, изображений, лайков и комментариев.

Стек технологий

• Java 21
• Spring Framework 6 (Spring Web MVC, Spring JDBC, Spring Test) — без Spring Boot
• H2 Database (in-memory БД с автоматической инициализацией схемы через schema.sql)
• Apache Tomcat 10.1 (упаковка в ROOT.war)
• Gradle
• JUnit 5, Mockito, MockMvc

Сборка и запуск

1. Запуск тестов и сборка WAR-архива

    ./gradlew clean test war

    Готовый архив создаётся по пути build/libs/ROOT.war.

2. Деплой в Apache Tomcat 10.1

   2.1 Остановите Tomcat (bin/shutdown.bat или bin/shutdown.sh).
   2.2 Удалите стандартную папку webapps/ROOT и скопируйте build/libs/ROOT.war в папку webapps/.
   2.3 Запустите Tomcat (bin/startup.bat или bin/startup.sh).
   2.4 Бэкенд будет доступен по адресу http://localhost:8080.

3. Запуск с фронтендом (Docker)

    Запустите Docker-контейнер с клиентской частью, после чего откройте приложение в браузере по адресу http://localhost/.

Реализованные эндпоинты REST API

Посты

    • GET /api/posts?search=&pageNumber=1&pageSize=5 — список постов с поиском (по заголовку и #тегам) и пагинацией (превью текста обрезается до 128 символов)
    • GET /api/posts/{id} — получение полной информации о посте по ID
    • POST /api/posts — создание нового поста
    • PUT /api/posts/{id} — редактирование поста
    • DELETE /api/posts/{id} — удаление поста вместе со всеми его комментариями
    • POST /api/posts/{id}/likes — увеличение счётчика лайков поста на +1
    • PUT /api/posts/{id}/image — загрузка или обновление изображения поста (multipart/form-data, поле image)
    • GET /api/posts/{id}/image — получение изображения поста в виде массива байт

Комментарии

    • GET /api/posts/{id}/comments — список комментариев к посту
    • GET /api/posts/{id}/comments/{commentId} — получение одного комментария по ID
    • POST /api/posts/{id}/comments — добавление нового комментария к посту
    • PUT /api/posts/{id}/comments/{commentId} — редактирование комментария
    • DELETE /api/posts/{id}/comments/{commentId} — удаление комментария