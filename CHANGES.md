## 1.1.0 (24.02.2018)
- Добавлены вкладки для разных таймлайнов - новые, лучшие за неделю/месяц/год
- Возможность сделать фото прямо из приложения (без `startActivityForResult`) при добавлении поста
- Рефакторинг: вместо `MutableLiveData` с nullable параметром используется `SingleLiveEvent`

## 1.0.4 (17.02.2018)
- Для отображения ошибок `SelectImageFragment` вызывает `MainViewModel` 

## 1.0.3 (14.02.2018)
- Исправил проблему с multidex на android <= 20

## 1.0.2 (14.02.2018)
- Починил воспроизведение видео при некоторых ситуациях

## 1.0.1 (14.02.2018)
- Убрал обновлятор лайков из сервиса на `ViewModel` `MainActivity`

## 1.0.0 (13.02.2018)
- Первый релиз