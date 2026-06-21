
   <p align="center">
      <img width="192" height="192" alt="icon_launcher" src="https://github.com/user-attachments/assets/2f1930c5-8fd1-49dd-b992-e871e71d4c31" />
   </p>
   
# SKAT Motor Tester

Android-приложение для стенда **SKAT-Tenzo**: измерение силы тяги, тока и напряжения по Bluetooth LE, управление приводом ESC и запись телеметрии на графики.

**Версия:** 1.0.0  
**Совместимое устройство:** контроллер **SKAT-Tenzo** (имя в Bluetooth: `SKAT-Tenzo`)

> Прошивка ESP32 поставляется отдельно и в этот репозиторий не входит.

Прошивка ESP32 [SKAT-ESP32-Tenzo](https://github.com/reduct0r/SKAT-ESP32-Tenzo/tree/main)

---

## Системные требования

| Параметр | Требование |
|----------|------------|
| ОС | Android 8.0 (API 26) и выше |
| Bluetooth | BLE 4.0+ (обязательно) |
| Разрешения | Bluetooth, при необходимости — геолокация (Android 11 и ниже) |
| Устройство | Смартфон или планшет с поддержкой BLE |

Рекомендуется Android 12+ для стабильной работы без запроса геолокации при сканировании.

---

## Установка

1. Скачайте APK из раздела [Releases](https://github.com/reduct0r/SKAT-Load-cell/releases) (файл `SKAT-Motor-Tester-1.0.0.apk`).
2. Разрешите установку из неизвестных источников для браузера или файлового менеджера.
3. Установите приложение **SKAT Motor Tester**.

---

## Подключение к стенду

### Подготовка

1. Включите стенд SKAT-Tenzo (питание ESP32, датчики и ESC подключены).
2. Убедитесь, что на телефоне включены **Bluetooth** и **геолокация** (на старых версия Android).
3. Запустите приложение.

### Первое подключение

1. На главном экране нажмите **«Найти устройство»**.
   <p align="center">
     <img src="https://github.com/user-attachments/assets/c749187e-d159-4239-8ad4-8d5991c06a81" width="360" alt="Главный экран">
   </p>
3. При запросе разрешите:
   - **Bluetooth** (поиск и подключение устройств);
   - **Геолокация** — только на Android 11 и ниже (требование системы для BLE-сканирования).
4. В списке выберите **SKAT-Tenzo**.
   <p align="center">
     <img width="360" alt="2" src="https://github.com/user-attachments/assets/73330048-7696-4767-bac6-adf3ec928a19" />
   </p>
6. После подключения статус изменится на **«Подключено»**, отобразятся показания датчиков.
   <p align="center">
     <img width="360" alt="3" src="https://github.com/user-attachments/assets/ed856408-7472-47d2-9a93-adddcc37dbb2" />
   </p>


### Повторное подключение

Приложение не хранит калибровку — она сохраняется в контроллере. Достаточно снова выбрать устройство в списке сканирования.

---

## Работа с приложением

### Главный экран

   <p align="center">
    <img width="360" alt="4" src="https://github.com/user-attachments/assets/246f6622-c0f5-4728-bfc8-93ecff2e87f0" />
   </p>

- **Сила тяги (Н)** и **эквивалентная масса (г)** — тензодатчик HX711.
- **Ток (А)** и **напряжение (В)** — датчик INA226.
- **Мощность (Вт)** — расчёт U × I.
- **Графики** — краткий обзор; кнопка перехода к подробным графикам.
- **Arm / Disarm** — разрешение и запрет управления приводом.
- **Throttle** — ползунок мощности ESC (0–100 %). Активен только в состоянии **Arm**.

### Управление приводом

   <p align="center">
     <img width="360" alt="5" src="https://github.com/user-attachments/assets/43eb0bf5-b9ae-41bc-b64b-fa6536b4343f" />
   </p>

1. Подключитесь к стенду.
2. Нажмите **Arm** — привод переводится в режим готовности.

   <p align="center">
    <img width="360" alt="6" src="https://github.com/user-attachments/assets/3de18102-378e-42f4-8154-845c3f5478ea" />
   </p>

4. Двигайте **Throttle** для изменения мощности.
5. Для эксренной остановки нажмите **Disarm** — мощность сбрасывается в ноль.

При разрыве Bluetooth-соединения привод автоматически переводится в безопасное состояние (disarm на контроллере), слайдер в приложении сбрасывается.

### Графики и экспорт

   <p align="center">
     <img width="360" alt="7" src="https://github.com/user-attachments/assets/b8a8bdd5-a72e-4de0-882d-ca254cb090fa" />
   </p>

- На главном экране — сводные графики силы, тока, напряжения. При нажатии на шестеренку есть возможность вкл/выкл отображение значений.

   <p align="center">
    <img width="360" alt="8" src="https://github.com/user-attachments/assets/d867941f-b309-4668-90df-77991873cc1c" />
   </p>
  
- **Графики — подробно** — отдельные графики по каждому параметру.

   <p align="center">
    <img height="360" alt="10" src="https://github.com/user-attachments/assets/ec1d2d3b-73fb-4eaf-af9c-bdc7b6244d4d" />
   </p>
  
- Нажатие на график — **полноэкранный режим** (альбомная ориентация).

   <p align="center">
      <img width="360" alt="9" src="https://github.com/user-attachments/assets/79155f7c-faba-44d0-bc23-687d7b683738" />
   </p>
  
- Экспорт **CSV** и **PNG** доступен на экранах графиков.

---

## Калибровка

   <p align="center">
      <img width="360" alt="11" src="https://github.com/user-attachments/assets/c77daa5b-dafd-49a7-9e85-f44b9d8e8598" />
   </p>

Откройте **настройки** (иконка шестерёнки на главном экране). Требуется активное BLE-подключение.

### Тензодатчик HX711

   <p align="center">
      <img width="360" alt="12" src="https://github.com/user-attachments/assets/9a88941e-96e0-4096-85a2-5638d496caa9" />
   </p>

1. Снять нагрузку со стенда.
2. **«Обнулить»** — установить нулевую точку (0 г).
3. Установить **эталонную массу** (не снимать до конца калибровки).
4. Ввести массу в граммах → **«Калибровать по массе»**.
5. При необходимости включить **«Инвертировать знак силы»**, если направление отсчёта обратное.

**«Сбросить шкалу»** — возврат к параметрам по умолчанию и повторное обнуление.

### Датчик тока INA226

   <p align="center">
      <img width="360" alt="13" src="https://github.com/user-attachments/assets/1b9dbd56-eee7-4e58-83c9-60b6ae652072" />
   </p>

1. **Шунт:** ввести сопротивление в омах (например, `0,0036`) → **«Применить шунт»** → **«Перекалибровать INA226»**.
2. **Напряжение:** измерить мультиметром на клеммах питания → ввести значение → **«Калибровать напряжение»**.
3. **Ток:** при выключенном приводе (Disarm) и без нагрузки → **«Обнулить ток»**.
4. При необходимости — **«Инвертировать знак тока»**.

Калибровка сохраняется в контроллере SKAT-Tenzo и сохраняется после перезагрузки стенда.

---

## Сборка из исходников

```bash
git clone https://github.com/reduct0r/SKAT-Load-cell.git
cd SKAT-Load-cell
./gradlew assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`  
Для установки на устройство подпишите APK или используйте debug-сборку: `./gradlew assembleDebug`.

### Стек

- Kotlin, Jetpack Compose, Material 3
- Hilt, Nordic Android BLE Library
- Navigation 3

---

## Лицензия

Проект распространяется в рамках репозитория [reduct0r/SKAT-Load-cell](https://github.com/reduct0r/SKAT-Load-cell).

---

## Поддержка

Ошибки и предложения — через [Issues](https://github.com/reduct0r/SKAT-Load-cell/issues) на GitHub.
