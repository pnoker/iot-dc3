#
#  Copyright 2018-2020 Pnoker. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

Default:
    echo "make dev-dc3: mvn mvn clean package & docker-compose build & docker-compose up"
    echo "make deploy-dc3: mvn mvn clean package deploy"
    echo "make package-dc3: mvn clean package"
    echo "make clean: mvn clean"

.PHONY: build clean-dc3

dev-dc3:
	cd dc3/bin \
	&& chmod +x mvn-clean.sh \
	&& chmod +x mvn-package.sh \
	&& ./mvn-clean.sh \
	&& ./mvn-package.sh \
	&& cd dev \
	&& chmod +x docker-compose-build.sh \
	&& chmod +x docker-compose-up.sh \
	&& ./docker-compose-build.sh \
	&& ./docker-compose-up.sh \

deploy-dc3:
	cd dc3/bin \
	&& chmod +x mvn-clean.sh \
	&& chmod +x mvn-package.sh \
	&& chmod +x mvn-deploy.sh \
	&& ./mvn-clean.sh \
	&& ./mvn-package.sh \
	&& ./mvn-deploy.sh

package-dc3:
	cd dc3/bin \
	&& chmod +x mvn-clean.sh \
	&& chmod +x mvn-package.sh \
	&& ./mvn-clean.sh \
	&& ./mvn-package.sh

clean-dc3:
	cd dc3/bin \
	&& chmod +x mvn-clean.sh \
	&& ./mvn-clean.sh