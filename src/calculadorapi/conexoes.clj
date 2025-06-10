(ns calculadorapi.conexoes
    (:require [clj-http.client :as client]
              [cheshire.core :as json]))

(def BURNED_API_URL "https://api.api-ninjas.com/v1/caloriesburned?activity=%s")
(def BURNED_API_KEY "YIgc/1iXCMsI/SmYbZ0VQw==X3HElntYRgfcow86")

(def TRANSLATE_API_URL "https://api-free.deepl.com/v2/translate")
(def TRANSLATE_API_KEY "26682f17-5995-417f-9a78-761af414c83c:fx")

(def CALORIES_API_URL "https://caloriasporalimentoapi.herokuapp.com/api/calorias/?descricao=%s")


(defn pegar-gasto-calorias [atividade peso duracao]
    (let [url (format BURNED_API_URL atividade)
          params (cond-> {:activity atividade}
                 peso (assoc :weight peso)
                 duracao (assoc :duration duracao))
          body (client/get url {:query-params params
                                :headers {:X-Api-Key BURNED_API_KEY}})]
        (if (= 200 (:status body))
            (json/parse-string (:body body) true)
            {:error "Erro ao acessar a API de queima de calorias"})      
    )
)

(defn pegar-atividades [atividade]
    (let [url (format BURNED_API_URL atividade)
          body (client/get url {:headers {:X-Api-Key BURNED_API_KEY}})]
        (if (= 200 (:status body))
            (json/parse-string (:body body) true)
            {:error "Erro ao acessar a API de queima de calorias"})      
    )
)

(defn pegar-ganho-calorias [comida]
  (let [resposta (client/get (format CALORIES_API_URL comida))]
          (if (= 200 (:status resposta))
            (json/parse-string (:body resposta) true)
            {:error "Erro ao acessar a API de calorias"})
          
      )
)

(defn traduzir [text origem destino]
    (let [params (cond-> {:text [text]
                          :target_lang destino}
                   origem (assoc :source_lang origem))
          resposta (client/post TRANSLATE_API_URL
                                {:headers {"Authorization" (str "DeepL-Auth-Key " TRANSLATE_API_KEY)
                                           "Content-Type" "application/json"}
                                 :body (json/generate-string params)
                                 :as :json})]
      (if (= 200 (:status resposta))
        (:body resposta) 
        {:error (str "Erro na tradução. Código HTTP: " (:status resposta))})))