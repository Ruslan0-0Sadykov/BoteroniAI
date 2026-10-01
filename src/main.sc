require: slotfilling/slotFilling.sc
  module = sys.zb-common
theme: /

    state: Start
        q!: $regex</start>
        a: Привет! Я BoteroniAI — помогу оформить заказ пиццы.
        a: Можешь сразу сказать, какую пиццу хочешь, например: "Хочу большую пиццу на тонком тесте с пепперони".
        script:
            $session.editingOrder = false;
            $session.size = null;
            $session.dough = null;
            $session.toppings = [];
            $session.sauce = null;
            $session.quantity = 1;
            $session.deliveryMethod = null;
            $session.address = null;
            $session.phone = null;
            $session.orderConfirmed = false;
            $session.addressText = null;
            
    state: AskSize
        a: Какой размер пиццы выбрать: маленький, средний или большой?
    
        state: GetSize
            q: * @PizzaSize::size *
            script:
                $session.size = $parseTree._size;
            a: Хорошо, размер — {{$session.size.name}}.
        
            if: $session.editingOrder
                script:
                    $session.editingOrder = false;
                go!: /OrderSummary
        
            if: !$session.dough
                go!: /AskDough  
        
    state: AskDough
        a: Какое тесто выбрать: тонкое или традиционное?
    
        state: GetDough
            q: * @PizzaDough::dough *
            script:
                $session.dough = $parseTree._dough;
            a: Хорошо, тесто — {{$session.dough.name}}.
            
            if: $session.editingOrder
                script:
                    $session.editingOrder = false;
                go!: /OrderSummary
            
            if: !$session.toppings || $session.toppings.length == 0
                go!: /AskTopping
            
    state: AskTopping
        a: Какую начинку добавить: пепперони, сыр, грибы или ветчину?
    
        state: GetTopping
            q: * @PizzaTopping::topping *
            script:
                $session.toppings = [$parseTree._topping];
            a: Хорошо, начинка — {{$session.toppings[0].name}}.
            
            if: $session.editingOrder
                script:
                    $session.editingOrder = false;
                go!: /OrderSummary
            
            if: !$session.deliveryMethod
                go!: /AskDelivery
                
    state: AskDelivery
        a: Как вы хотите получить заказ: доставка или самовывоз?
    
        state: GetDelivery
            q: * @DeliveryMethod::method *
            script:
                $session.deliveryMethod = $parseTree._method;
    
            if: $session.deliveryMethod.code == "delivery"
                a: Хорошо, оформим доставку.
                go!: /AskAddress
            if: $session.deliveryMethod.code == "pickup"
                a: Хорошо, заказ будет подготовлен к самовывозу.
                
                if: !$session.phone
                    go!: /AskPhone
                
    state: AskAddress
        a: Назовите адрес доставки, например: улица Ленина, дом 25.
    
        state: GetAddress
            q: * @DeliveryAddress::address *
            script:
                $session.address = $parseTree._address;
                $session.addressText = $request.query;
            a: Хорошо, адрес доставки сохранён: {{$request.query}}.
            
            if: !$session.phone
                go!: /AskPhone
            
    state: AskPhone
        a: Укажите номер телефона для связи.
    
        state: GetPhone
            q: * @duckling.phone-number::phone *
            script:
                $session.phone = $parseTree._phone;
            a: Хорошо, номер телефона сохранён: {{$session.phone}}.
            go!: /OrderSummary
            
    state: AskSauce
        a: Какой соус выбрать: томатный, сырный или чесночный?
    
        state: GetSauce
            q: * @PizzaSauce::sauce *
            script:
                $session.sauce = $parseTree._sauce;
            a: Хорошо, соус — {{$session.sauce.name}}.
    
            if: $session.editingOrder
                script:
                    $session.editingOrder = false;
                go!: /OrderSummary
            
    state: OrderSummary
        script:
            $session.toppingsText = "";
    
            for (var i = 0; i < $session.toppings.length; i++) {
                if (i > 0) {
                    $session.toppingsText += ", ";
                }
    
                $session.toppingsText += $session.toppings[i].name;
            }
    
            $session.sauceText = "не выбран";
            if ($session.sauce) {
                $session.sauceText = $session.sauce.name;
            }
    
        a: Проверьте ваш заказ:
        a: Количество: {{$session.quantity}}
        a: Размер: {{$session.size.name}}
        a: Тесто: {{$session.dough.name}}
        a: Начинка: {{$session.toppingsText}}
        a: Соус: {{$session.sauceText}}
        a: Способ получения: {{$session.deliveryMethod.name}}
    
        if: $session.deliveryMethod.code == "delivery"
            a: Адрес: {{$session.addressText}}
    
        a: Телефон: {{$session.phone}}
        a: Всё верно? Подтвердите заказ.
        
        state: Confirm
            intent: /ConfirmOrder
            script:
                $session.orderConfirmed = true;
        
            a: Заказ подтверждён!
            a: Начинаем готовить вашу пиццу.
        
            if: $session.deliveryMethod.code == "delivery"
                a: После приготовления заказ будет передан курьеру по адресу {{$session.addressText}}.
        
            if: $session.deliveryMethod.code == "pickup"
                a: После приготовления заказ можно будет забрать самостоятельно.
            
        state: Cancel
            intent: /CancelOrder
            script:
                $session.size = null;
                $session.dough = null;
                $session.toppings = [];
                $session.sauce = null;
                $session.quantity = 1;
                $session.deliveryMethod = null;
                $session.address = null;
                $session.addressText = null;
                $session.phone = null;
                $session.orderConfirmed = false;
            a: Заказ отменён. Все данные заказа очищены.
            a: Чтобы оформить новый заказ, напишите /start.
            
        state: Change
            intent: /ChangeOrder
        
            if: !$parseTree._size && !$parseTree._dough && !$parseTree._toppings && !$parseTree._sauce
                script:
                    $session.editingOrder = true;
                go!: /AskWhatChange
        
            script:
                if ($parseTree._size) {
                    $session.size = $parseTree._size;
                }
        
                if ($parseTree._dough) {
                    $session.dough = $parseTree._dough;
                }
        
                if ($parseTree._toppings) {
                    $session.toppings = [$parseTree._toppings];
                }
        
                if ($parseTree._sauce) {
                    $session.sauce = $parseTree._sauce;
                }
        
            a: Изменения сохранены.
            go!: /OrderSummary
            
    state: AskWhatChange
        a: Что вы хотите изменить: размер, тесто, начинку или соус?
    
        state: ChangeSize
            q: * размер *
            go!: /AskSize
    
        state: ChangeDough
            q: * тесто *
            go!: /AskDough
    
        state: ChangeTopping
            q: * начинк* *
            go!: /AskTopping
            
        state: ChangeSauce
            q: * соус *
            go!: /AskSauce
    
    state: OrderPizzaState
        intent!: /OrderPizza
        script:
            if ($parseTree._size) {
                $session.size = $parseTree._size;
            }
    
            if ($parseTree._dough) {
                $session.dough = $parseTree._dough;
            }
    
            if ($parseTree._toppings) {
                $session.toppings = $parseTree._toppings;
            }
    
            if ($parseTree._sauce) {
                $session.sauce = $parseTree._sauce;
            }
    
            if ($parseTree._quantity) {
                $session.quantity = $parseTree._quantity;
            }
            
            $session.sizeText = "не указан";
                if ($session.size) {
                    $session.sizeText = $session.size.name;
                }
                
                $session.doughText = "не указано";
                if ($session.dough) {
                    $session.doughText = $session.dough.name;
                }
                
                $session.sauceText = "не указан";
                if ($session.sauce) {
                    $session.sauceText = $session.sauce.name;
                }
                
                $session.toppingsText = "не указана";
                if ($session.toppings && $session.toppings.length > 0) {
                    $session.toppingsText = "";
                
                    for (var i = 0; i < $session.toppings.length; i++) {
                        if (i > 0) {
                            $session.toppingsText += ", ";
                        }
                
                        $session.toppingsText += $session.toppings[i].name;
                    }
                }
        a: Размер: {{$session.sizeText}}
        a: Тесто: {{$session.doughText}}
        a: Количество: {{$session.quantity}}
        a: Начинка: {{$session.toppingsText}}
        a: Соус: {{$session.sauceText}}
        
        if: !$session.size
            go!: /AskSize
        if: !$session.dough
            go!: /AskDough
        if: !$session.toppings || $session.toppings.length == 0
            go!: /AskTopping
        if: !$session.deliveryMethod
            go!: /AskDelivery
            
    state: Hello
        intent!: /привет
        a: Привет привет

    state: Bye
        intent!: /пока
        a: Пока пока

    state: NoMatch
        event!: noMatch
        a: Я не понял. Вы сказали: {{$request.query}}

    state: Match
        event!: match
        a: {{$context.intent.answer}}