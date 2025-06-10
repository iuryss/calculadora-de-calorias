(ns calculadorapi.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as json]
            [ring.middleware.defaults :refer [wrap-defaults
                                              api-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [calculadorapi.db :as db]
            [calculadorapi.conexoes :as conexoes]))

(defn como-json [conteudo & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/generate-string conteudo)})
  

(defn registro-valido? [registro]
  (and (contains? registro :tipo)
       (contains? registro :descricao)
       (contains? registro :data)
       (contains? registro :quantidade)
       (or (= (:tipo registro) "ganho")
           (= (:tipo registro) "perda")))) 

(defn registro-usuario-valido? [registro]
  (and (contains? registro :nome)
       (contains? registro :peso)
       (contains? registro :altura)
       (contains? registro :sexo)
       (contains? registro :idade)))

(defn intervalo-valido? [registro]
  (and (contains? registro :dataInicio) 
       (contains? registro :dataFim))
)

(defroutes app-routes
  (GET "/" [] "hello world")
  (GET "/ganhos" [] (como-json {:registros (db/registros-do-tipo "ganho")}))
  (GET "/perdas" [] (como-json {:registros (db/registros-do-tipo "perda")}))
  (GET "/registros" [] (como-json {:registros (db/registros)}))
  (GET "/saldo" [] (como-json {:saldo (db/saldo)}))
  (GET "/alimentos" [descricao] 
      (if descricao
        (como-json {:alimentos (db/pegar-alimentos descricao)})
        (como-json {:mensagem "Descrição do alimento é necessária"} 400)))
  (GET "/exercicios" [descricao]
      (if descricao
        (como-json {:exercicios (db/pegar-exercicios descricao)})
        (como-json {:mensagem "Descrição do exercício é necessária"} 400)))
  (GET "/saldo-do-periodo" [inicio fim]
    (let [inicio (str inicio)
          fim (str fim)]
      (if (and inicio fim)
        (como-json {:saldo (db/saldo-do-periodo inicio fim)})
        (como-json {:mensagem "Data inválida. Use o formato dd/MM/yyyy."} 400))))
  (GET "/registro-do-periodo" [inicio fim]
    (let [inicio (str inicio)
          fim (str fim)]
      (if (and inicio fim)
        (como-json {:registros (db/registros-do-periodo inicio fim)})
        (como-json {:mensagem "Data inválida. Use o formato dd/MM/yyyy."} 400))))
  (POST "/registrar" requisicao 
        (if (registro-valido? (:body requisicao))
          (-> (db/novo-registro (:body requisicao))
              (como-json 201))
          (como-json {:mensagem "Registro inválido"} 400))) 
  (POST "/registrar-usuario" requisicao 
        (if (registro-usuario-valido? (:body requisicao))
          (-> (db/registrar-usuario (:body requisicao))
              (como-json 201))
          (como-json {:mensagem "Registro inválido"} 400))) 
  (DELETE "/limpar" [] (do (db/limpar) (como-json {:mensagem ""} 204))))
  

(def app
  (-> (wrap-defaults app-routes api-defaults)
      (wrap-json-body {:keywords? true :bigdecimals? true})))



