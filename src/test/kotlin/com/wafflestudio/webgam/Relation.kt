package com.wafflestudio.webgam

import com.wafflestudio.webgam.domain.event.dto.ObjectEventDto
import com.wafflestudio.webgam.domain.event.model.ObjectEvent
import com.wafflestudio.webgam.domain.event.model.TransitionType
import com.wafflestudio.webgam.domain.`object`.dto.PageObjectDto
import com.wafflestudio.webgam.domain.`object`.model.PageObject
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType
import com.wafflestudio.webgam.domain.`object`.model.PageObjectType.DEFAULT
import com.wafflestudio.webgam.domain.page.dto.ProjectPageDto
import com.wafflestudio.webgam.domain.page.model.ProjectPage
import com.wafflestudio.webgam.domain.project.dto.ProjectDto
import com.wafflestudio.webgam.domain.project.model.Project
import com.wafflestudio.webgam.domain.user.model.User

object Relation {
    infix fun withUser(name: String) = UserConfiguration(User("$name-id", name, "$name@email.com", ""))
    interface Configuration {
        fun build(): User
    }
    class UserConfiguration(private val user: User): Configuration {
        override fun build() = user
        infix fun deleted(boolean: Boolean): UserConfiguration {
            if (boolean) user.delete()
            return this
        }
        infix fun withProject(name: String) = ProjectConfiguration(user, name, this)
    }

    class ProjectConfiguration(private val project: Project, private val chain: UserConfiguration): Configuration {
        constructor(user: User, name: String, chain: UserConfiguration): this(
            project = Project(user, ProjectDto.CreateRequest(name)),
            chain = chain
        )
        override fun build() = chain.build()
        infix fun deleted(boolean: Boolean): ProjectConfiguration {
            if (boolean) project.delete()
            return this
        }
        infix fun withProject(name: String) = chain.withProject(name)
        infix fun withPage(name: String) = PageConfiguration(project, name, this)
    }

    class PageConfiguration(private val page: ProjectPage, private val chain: ProjectConfiguration): Configuration {
        constructor(project: Project, name: String, chain: ProjectConfiguration): this(
            page = ProjectPage(project, ProjectPageDto.CreateRequest(0L, name)),
            chain = chain
        )
        override fun build() = chain.build()
        infix fun deleted(boolean: Boolean): PageConfiguration {
            if (boolean) page.delete()
            return this
        }
        infix fun withProject(name: String) = chain.withProject(name)
        infix fun withPage(name: String) = chain.withPage(name)
        infix fun withObject(name: String) = ObjectConfiguration(page, name, this)
    }

    class ObjectConfiguration(private val obj: PageObject, private val chain: PageConfiguration): Configuration {
        constructor(page: ProjectPage, name: String, chain: PageConfiguration) : this(
            obj = PageObject(page, PageObjectDto.CreateRequest(0L, name, DEFAULT, 0, 0, 0, 0, 0, null, null, null)),
            chain = chain
        )
        override fun build() = chain.build()
        infix fun deleted(boolean: Boolean): ObjectConfiguration {
            if (boolean) obj.delete()
            return this
        }
        infix fun type(type: PageObjectType): ObjectConfiguration {
            obj.type = type
            return this
        }
        infix fun withProject(name: String) = chain.withProject(name)
        infix fun withPage(name: String) = chain.withPage(name)
        infix fun withObject(name: String) = chain.withObject(name)
        infix fun withEvent(type: TransitionType) = EventConfiguration(obj, type, this)
    }

    class EventConfiguration(private val event: ObjectEvent, private val chain: ObjectConfiguration): Configuration {
        constructor(obj: PageObject, type: TransitionType, chain: ObjectConfiguration): this(
            event = ObjectEvent(ObjectEventDto.CreateRequest(0L, type, 0L), obj),
            chain = chain
        )
        override fun build() = chain.build()
        infix fun deleted(boolean: Boolean): EventConfiguration {
            if (boolean) event.delete()
            return this
        }
        infix fun withNextPage(name: String): EventConfiguration {
            event.nextPage = event.`object`.page.project.pages.first { it.name == name }
            return this
        }
        infix fun withProject(name: String) = chain.withProject(name)
        infix fun withPage(name: String) = chain.withPage(name)
        infix fun withObject(name: String) = chain.withObject(name)
        infix fun withEvent(type: TransitionType) = chain.withEvent(type)
    }
}
