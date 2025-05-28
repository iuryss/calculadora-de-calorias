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
       (or (= (:tipo registro) "ganho")
           (= (:tipo registro) "perda"))))

(defn traducao-valida? [traducao]
  (and (contains? traducao :texto)
       (contains? traducao :origem)
       (contains? traducao :destino))) ;;melhorar validação de data

(defroutes app-routes
  (GET "/" [] "hello world")
  (GET "/saldo" [] (como-json {:saldo (db/saldo)}))
  (GET "/saldo-do-dia" requisicao
  (let [body (:body requisicao)]
    (if (contains? body :data)
      (let [data (:data body)]
        (como-json {:saldo (db/saldo-do-dia data)}))
      (como-json {:mensagem "Data inválida. Use o formato dd/MM/yyyy."} 400)))) ;; criar saldos da semana, mês, ano
  (POST "/registrar" requisicao 
        (if (registro-valido? (:body requisicao))
          (-> (db/novo-registro (:body requisicao))
              (como-json 201))
          (como-json {:mensagem "Registro inválido"} 400)))
  (GET "/ganhos" [] (como-json {:registros (db/registros-do-tipo "ganho")}))
  (GET "/perdas" [] (como-json {:registros (db/registros-do-tipo "perda")}))
  (GET "/registros" [] (como-json {:registros (db/registros)}))
  (DELETE "/limpar" [] (do (db/limpar) (como-json {:mensagem ""} 204))))

(def app
  (-> (wrap-defaults app-routes api-defaults)
      (wrap-json-body {:keywords? true :bigdecimals? true})))



